package com.acertainbookstore.business;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.BookStoreLockMap;
import com.acertainbookstore.utils.BookStoreUtility;

/** {@link TwoLevelLockingConcurrentCertainBookStore} implements the {@link BookStore} and
 * {@link StockManager} functionalities.
 * 
 * @see BookStore
 * @see StockManager
 */
public class TwoLevelLockingConcurrentCertainBookStore implements BookStore, StockManager {

	private static final ReadWriteLock dbLock = new ReentrantReadWriteLock(true);

	/** The mapping of books from ISBN to {@link BookStoreBook}. */
	private Map<Integer, BookStoreBook> bookMap = null;
	private BookStoreLockMap lockMap = null;


	/**
	 * Instantiates a new {@link CertainBookStore}.
	 */
	public TwoLevelLockingConcurrentCertainBookStore() {
		// Constructors are not synchronized
		bookMap = new HashMap<>();
		lockMap = new BookStoreLockMap();
	}
	
	private void validate(StockBook book) throws BookStoreException {
		int isbn = book.getISBN();
		String bookTitle = book.getTitle();
		String bookAuthor = book.getAuthor();
		int noCopies = book.getNumCopies();
		float bookPrice = book.getPrice();

		if (BookStoreUtility.isInvalidISBN(isbn)) { // Check if the book has valid ISBN
			throw new BookStoreException(BookStoreConstants.ISBN + isbn + BookStoreConstants.INVALID);
		}

		if (BookStoreUtility.isEmpty(bookTitle)) { // Check if the book has valid title
			throw new BookStoreException(BookStoreConstants.BOOK + book.toString() + BookStoreConstants.INVALID);
		}

		if (BookStoreUtility.isEmpty(bookAuthor)) { // Check if the book has valid author
			throw new BookStoreException(BookStoreConstants.BOOK + book.toString() + BookStoreConstants.INVALID);
		}

		if (BookStoreUtility.isInvalidNoCopies(noCopies)) { // Check if the book has at least one copy
			throw new BookStoreException(BookStoreConstants.BOOK + book.toString() + BookStoreConstants.INVALID);
		}

		if (bookPrice < 0.0) { // Check if the price of the book is valid
			throw new BookStoreException(BookStoreConstants.BOOK + book.toString() + BookStoreConstants.INVALID);
		}

		if (bookMap.containsKey(isbn)) {// Check if the book is already added
			throw new BookStoreException(BookStoreConstants.ISBN + isbn + BookStoreConstants.DUPLICATED);
		}
	}	
	
	private void validate(BookCopy bookCopy) throws BookStoreException {
		int isbn = bookCopy.getISBN();
		int numCopies = bookCopy.getNumCopies();

		validateISBNInStock(isbn); // Check if the book has valid ISBN and in stock

		if (BookStoreUtility.isInvalidNoCopies(numCopies)) { // Check if the number of the book copy is larger than zero
			throw new BookStoreException(BookStoreConstants.NUM_COPIES + numCopies + BookStoreConstants.INVALID);
		}
	}
	
	private void validate(BookEditorPick editorPickArg) throws BookStoreException {
		int isbn = editorPickArg.getISBN();
		validateISBNInStock(isbn); // Check if the book has valid ISBN and in stock
	}
	
	private void validateISBNInStock(Integer ISBN) throws BookStoreException {
		if (BookStoreUtility.isInvalidISBN(ISBN)) { // Check if the book has valid ISBN
			throw new BookStoreException(BookStoreConstants.ISBN + ISBN + BookStoreConstants.INVALID);
		}
		if (!bookMap.containsKey(ISBN)) {// Check if the book is in stock
			throw new BookStoreException(BookStoreConstants.ISBN + ISBN + BookStoreConstants.NOT_AVAILABLE);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.acertainbookstore.interfaces.StockManager#addBooks(java.util.Set)
	 */
	public void addBooks(Set<StockBook> bookSet) throws BookStoreException {
		if (bookSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		dbLock.writeLock().lock();
		// Check if all are there
		for (StockBook book : bookSet) {
			try {
				validate(book);
			} catch (BookStoreException e) {
				dbLock.writeLock().unlock();
				throw e;
			}
		}

		for (StockBook book : bookSet) {
			int isbn = book.getISBN();
			bookMap.put(isbn, new BookStoreBook(book));
		}

		List<Integer> isbns = bookSet.stream().map(StockBook::getISBN).collect(Collectors.toList());
		lockMap.addLocks(isbns);
		dbLock.writeLock().unlock();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.acertainbookstore.interfaces.StockManager#addCopies(java.util.Set)
	 */
	public void addCopies(Set<BookCopy> bookCopiesSet) throws BookStoreException {
		int isbn;
		int numCopies;

		if (bookCopiesSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		dbLock.readLock().lock();

		for (BookCopy bookCopy : bookCopiesSet) {
			try {
				validate(bookCopy);
			} catch (BookStoreException e) {
				dbLock.readLock().unlock();
				throw e;
			}
		}


		List<Integer> isbns = bookCopiesSet.stream().map(BookCopy::getISBN).collect(Collectors.toList());
		lockMap.writeLock(isbns);

		// Update the number of copies
		for (BookCopy bookCopy : bookCopiesSet) {
			isbn = bookCopy.getISBN();
			numCopies = bookCopy.getNumCopies();
			BookStoreBook book = bookMap.get(isbn);

			book.addCopies(numCopies);
		}

		lockMap.writeUnlock(isbns);

		dbLock.readLock().unlock();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.StockManager#getBooks()
	 */
	public List<StockBook> getBooks() {
		dbLock.readLock().lock();
		Collection<BookStoreBook> bookMapValues = bookMap.values();

		List<Integer> isbns = bookMapValues.stream().map(BookStoreBook::getISBN).collect(Collectors.toList());
		lockMap.readLock(isbns);

		List<StockBook> books = bookMapValues.stream()
				.map(book -> book.immutableStockBook())
				.collect(Collectors.toList());

		lockMap.readUnlock(isbns);
		dbLock.readLock().unlock();
		return books;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.acertainbookstore.interfaces.StockManager#updateEditorPicks(java.util
	 * .Set)
	 */
	public void updateEditorPicks(Set<BookEditorPick> editorPicks) throws BookStoreException {
		// Check that all ISBNs that we add/remove are there first.
		if (editorPicks == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		dbLock.readLock().lock();
		for (BookEditorPick editorPickArg : editorPicks) {
			try {
				validate(editorPickArg);
			} catch (BookStoreException e) {
				dbLock.readLock().unlock();
				throw e;
			}
		}

		List<Integer> isbns = editorPicks.stream().map(BookEditorPick::getISBN).collect(Collectors.toList());
		lockMap.readLock(isbns);

		for (BookEditorPick editorPickArg : editorPicks) {
			bookMap.get(editorPickArg.getISBN()).setEditorPick(editorPickArg.isEditorPick());
		}

		lockMap.readUnlock(isbns);
		dbLock.readLock().unlock();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.BookStore#buyBooks(java.util.Set)
	 */
	public void buyBooks(Set<BookCopy> bookCopiesToBuy) throws BookStoreException {
		if (bookCopiesToBuy == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}
		dbLock.readLock().lock();

		for (BookCopy bookCopy : bookCopiesToBuy) {
			try {
				validate(bookCopy);
			} catch (BookStoreException e) {
				dbLock.readLock().unlock();
				throw e;
			}
		}

		List<Integer> isbns = bookCopiesToBuy.stream().map(BookCopy::getISBN).collect(Collectors.toList());
		lockMap.writeLock(isbns);

		Map<Integer, Integer> salesMisses = new HashMap<>();
		// Check that all ISBNs that we buy are there first.
		int isbn;
		BookStoreBook book;
		boolean saleMiss = false;
		for (BookCopy bookCopyToBuy : bookCopiesToBuy) {
			isbn = bookCopyToBuy.getISBN();

			book = bookMap.get(isbn);

			if (!book.areCopiesInStore(bookCopyToBuy.getNumCopies())) {
				// If we cannot sell the copies of the book, it is a miss.
				salesMisses.put(isbn, bookCopyToBuy.getNumCopies() - book.getNumCopies());
				saleMiss = true;
			}
		}

		// We throw exception now since we want to see how many books in the
		// order incurred misses which is used by books in demand
		if (saleMiss) {
			for (Map.Entry<Integer, Integer> saleMissEntry : salesMisses.entrySet()) {
				book = bookMap.get(saleMissEntry.getKey());
				book.addSaleMiss(saleMissEntry.getValue());
			}

			lockMap.writeUnlock(isbns);
			dbLock.readLock().unlock();
			throw new BookStoreException(BookStoreConstants.BOOK + BookStoreConstants.NOT_AVAILABLE);
		}

		// Then make the purchase.
		for (BookCopy bookCopyToBuy : bookCopiesToBuy) {
			book = bookMap.get(bookCopyToBuy.getISBN());
			book.buyCopies(bookCopyToBuy.getNumCopies());
		}

		lockMap.writeUnlock(isbns);
		dbLock.readLock().unlock();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.acertainbookstore.interfaces.StockManager#getBooksByISBN(java.util.
	 * Set)
	 */
	public List<StockBook> getBooksByISBN(Set<Integer> isbnSet) throws BookStoreException {
		if (isbnSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		dbLock.readLock().lock();

		for (Integer ISBN : isbnSet) {
			try {
				validateISBNInStock(ISBN);
			}
			catch (BookStoreException e) {
				dbLock.readLock().unlock();
				throw e;
			}
		}

		List<Integer> isbns = new ArrayList<>(isbnSet);
		lockMap.readLock(isbns);

		List <StockBook> books = isbnSet.stream()
				.map(isbn -> bookMap.get(isbn).immutableStockBook())
				.collect(Collectors.toList());

		lockMap.readUnlock(isbns);
		dbLock.readLock().unlock();

		return books;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.BookStore#getBooks(java.util.Set)
	 */
	public List<Book> getBooks(Set<Integer> isbnSet) throws BookStoreException {
		if (isbnSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		dbLock.readLock().lock();

		// Check that all ISBNs that we rate are there to start with.
		for (Integer ISBN : isbnSet) {
			try {
				validateISBNInStock(ISBN);
			} catch (BookStoreException e) {
				dbLock.readLock().unlock();
				throw e;
			}
		}

		List<Integer> isbns = new ArrayList<>(isbnSet);
		lockMap.readLock(isbns);

		List<Book> books = isbnSet.stream()
				.map(isbn -> bookMap.get(isbn).immutableBook())
				.collect(Collectors.toList());

		lockMap.readUnlock(isbns);
		dbLock.readLock().unlock();

		return books;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.BookStore#getEditorPicks(int)
	 */
	public List<Book> getEditorPicks(int numBooks) throws BookStoreException {
		if (numBooks < 0) {
			throw new BookStoreException("numBooks = " + numBooks + ", but it must be positive");
		}

		dbLock.readLock().lock();

		List<BookStoreBook> listAllEditorPicks = bookMap.entrySet().stream()
				.map(pair -> pair.getValue())
				.filter(book -> book.isEditorPick())
				.collect(Collectors.toList());

		dbLock.readLock().unlock();

		// Find numBooks random indices of books that will be picked.
		Random rand = new Random();
		Set<Integer> tobePicked = new HashSet<>();
		int rangePicks = listAllEditorPicks.size();

		if (rangePicks <= numBooks) {

			// We need to add all books.
			for (int i = 0; i < listAllEditorPicks.size(); i++) {
				tobePicked.add(i);
			}
		} else {

			// We need to pick randomly the books that need to be returned.
			int randNum;

			while (tobePicked.size() < numBooks) {
				randNum = rand.nextInt(rangePicks);
				tobePicked.add(randNum);
			}
		}

		// Return all the books by the randomly chosen indices.
		return tobePicked.stream()
				.map(index -> listAllEditorPicks.get(index).immutableBook())
				.collect(Collectors.toList());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.BookStore#getTopRatedBooks(int)
	 */
	@Override
	public List<Book> getTopRatedBooks(int numBooks) throws BookStoreException {
		throw new BookStoreException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.StockManager#getBooksInDemand()
	 */
	@Override
	public List<StockBook> getBooksInDemand() throws BookStoreException {
		throw new BookStoreException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.BookStore#rateBooks(java.util.Set)
	 */
	@Override
	public void rateBooks(Set<BookRating> bookRating) throws BookStoreException {
		throw new BookStoreException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.StockManager#removeAllBooks()
	 */
	public void removeAllBooks() throws BookStoreException {
		dbLock.writeLock().lock();
		lockMap.deleteAllLocks();
		bookMap.clear();
		dbLock.writeLock().unlock();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.acertainbookstore.interfaces.StockManager#removeBooks(java.util.Set)
	 */
	public void removeBooks(Set<Integer> isbnSet) throws BookStoreException {
		if (isbnSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}
		dbLock.writeLock().lock();

		for (Integer ISBN : isbnSet) {
			if (BookStoreUtility.isInvalidISBN(ISBN)) {
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN + BookStoreConstants.INVALID);
			}

			if (!bookMap.containsKey(ISBN)) {
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN + BookStoreConstants.NOT_AVAILABLE);
			}
		}

		List<Integer> isbnList = new ArrayList<>(isbnSet);
		lockMap.deleteLocks(isbnList);

		for (int isbn : isbnSet) {
			bookMap.remove(isbn);
		}

		dbLock.writeLock().unlock();
	}
}
