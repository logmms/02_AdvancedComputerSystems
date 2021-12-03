package com.acertainbookstore.client.tests;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.acertainbookstore.business.*;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;

/**
 * {@link BookStoreTest} tests the {@link BookStore} interface.
 * 
 * @see BookStore
 */
public class BookStoreTest {

	/** The Constant TEST_ISBN. */
	private static final int TEST_ISBN = 3044560;

	/** The Constant NUM_COPIES. */
	private static final int NUM_COPIES = 5;

	/** The local test. */
	private static boolean localTest = false;

	/** The store manager. */
	private static StockManager storeManager;

	/** The client. */
	private static BookStore client;

	/**
	 * Sets the up before class.
	 */
	@BeforeClass
	public static void setUpBeforeClass() {
		try {
			String localTestProperty = System.getProperty(BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
			localTest = (localTestProperty != null) ? Boolean.parseBoolean(localTestProperty) : localTest;

			if (localTest) {
				CertainBookStore store = new CertainBookStore();
				storeManager = store;
				client = store;
			} else {
				storeManager = new StockManagerHTTPProxy("http://localhost:8081/stock");
				client = new BookStoreHTTPProxy("http://localhost:8081");
			}

			storeManager.removeAllBooks();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Helper method to add some books.
	 *
	 * @param isbn
	 *            the isbn
	 * @param copies
	 *            the copies
	 * @throws BookStoreException
	 *             the book store exception
	 */
	public void addBooks(int isbn, int copies) throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		StockBook book = new ImmutableStockBook(isbn, "Test of Thrones", "George RR Testin'", (float) 10, copies, 0, 0,
				0, false);
		booksToAdd.add(book);
		storeManager.addBooks(booksToAdd);
	}

	public void rateBook(int isbn, int rating) throws BookStoreException {
		Set<BookRating> booksToRate = new HashSet<BookRating>();
		booksToRate.add(new BookRating(isbn, rating));
		client.rateBooks(booksToRate);
	}

	/**
	 * Helper method to add some books and rate them.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	public void addRatedBooks() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 1, 4, true));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 1, 3, true));

		storeManager.addBooks(booksToAdd);
	}

	/**
	 * Helper method to get the default book used by initializeBooks.
	 *
	 * @return the default book
	 */
	public StockBook getDefaultBook() {
		return new ImmutableStockBook(TEST_ISBN, "Harry Potter and JUnit", "JK Unit", (float) 10, NUM_COPIES, 0, 0, 0,
				false);
	}

	/**
	 * Method to add a book, executed before every test case is run.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Before
	public void initializeBooks() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(getDefaultBook());
		storeManager.addBooks(booksToAdd);
	}

	/**
	 * Method to clean up the book store, execute after every test case is run.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@After
	public void cleanupBooks() throws BookStoreException {
		storeManager.removeAllBooks();
	}

	/**
	 * Tests basic buyBook() functionality.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyAllCopiesDefaultBook() throws BookStoreException {
		// Set of books to buy
		Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES));

		// Try to buy books
		client.buyBooks(booksToBuy);

		List<StockBook> listBooks = storeManager.getBooks();
		assertTrue(listBooks.size() == 1);
		StockBook bookInList = listBooks.get(0);
		StockBook addedBook = getDefaultBook();

		assertTrue(bookInList.getISBN() == addedBook.getISBN() && bookInList.getTitle().equals(addedBook.getTitle())
				&& bookInList.getAuthor().equals(addedBook.getAuthor()) && bookInList.getPrice() == addedBook.getPrice()
				&& bookInList.getNumSaleMisses() == addedBook.getNumSaleMisses()
				&& bookInList.getAverageRating() == addedBook.getAverageRating()
				&& bookInList.getNumTimesRated() == addedBook.getNumTimesRated()
				&& bookInList.getTotalRating() == addedBook.getTotalRating()
				&& bookInList.isEditorPick() == addedBook.isEditorPick());
	}

	/**
	 * Tests that books with invalid ISBNs cannot be bought.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyInvalidISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a book with invalid ISBN.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1)); // valid
		booksToBuy.add(new BookCopy(-1, 1)); // invalid

		// Try to buy the books.
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();

		// Check pre and post state are same.
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that books can only be bought if they are in the book store.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyNonExistingISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a book with ISBN which does not exist.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1)); // valid
		booksToBuy.add(new BookCopy(100000, 10)); // invalid

		// Try to buy the books.
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();

		// Check pre and post state are same.
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that you can't buy more books than there are copies.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyTooManyBooks() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy more copies than there are in store.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES + 1));

		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that you can't buy a negative number of books.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyNegativeNumberOfBookCopies() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a negative number of copies.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, -1));

		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Test that several books can be rated
	 *
	 * @throws BookStoreException
	 */

	@Test
	public void testRateBooks() throws BookStoreException {
		// Add a book to the store
		addBooks(1, 1);

		// Set of books to rate
		Set<BookRating> ratings = new HashSet<BookRating>();
		ratings.add(new BookRating(TEST_ISBN, 3));
		ratings.add(new BookRating(1, 0));

		// Try to rate the books
		client.rateBooks(ratings);

		List<StockBook> listBooks = storeManager.getBooks();
		StockBook defaultBook = listBooks.get(0);
		StockBook addedBook = listBooks.get(1);

		assertTrue(defaultBook.getTotalRating() == 0
				&& defaultBook.getNumTimesRated() == 1
				&& defaultBook.getAverageRating() == 0);

		assertTrue(addedBook.getTotalRating() == 3
				&& addedBook.getNumTimesRated() == 1
				&& addedBook.getAverageRating() == 3);
	}

	/**
	 * Test that negative ratings are disallowed
	 *
	 * @throws BookStoreException
	 */

	@Test
	public void testNegativelyRateBooksAllOrNothing() throws BookStoreException {
		// Add a book to the store
		addBooks(1, 1);

		// Set of books to rate
		Set<BookRating> ratings = new HashSet<BookRating>();
		ratings.add(new BookRating(TEST_ISBN, 3)); // valid
		ratings.add(new BookRating(1, -3)); // invalid

		// Try to rate the books
		try {
			client.rateBooks(ratings);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> listBooks = storeManager.getBooks();
		StockBook defaultBook = listBooks.get(0);
		StockBook addedBook = listBooks.get(1);

		assertTrue(defaultBook.getTotalRating() == 0
				&& defaultBook.getNumTimesRated() == 0
				&& defaultBook.getAverageRating() == -1.0f);

		assertTrue(addedBook.getTotalRating() == 0
				&& addedBook.getNumTimesRated() == 0
				&& addedBook.getAverageRating() == -1.0f);
	}

	/**
	 * Test that ratings are disallowed for invalid ISBNs
	 *
	 * @throws BookStoreException
	 */

	@Test
	public void testInvalidISBNRateBooksAllOrNothing() throws BookStoreException {
		// Set of books to rate
		Set<BookRating> ratings = new HashSet<BookRating>();
		ratings.add(new BookRating(TEST_ISBN, 3)); // valid
		ratings.add(new BookRating(-1, 3)); // invalid

		// Try to rate the books
		try {
			client.rateBooks(ratings);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> books = storeManager.getBooks();
		StockBook defaultBook = books.get(0);

		assertTrue(defaultBook.getTotalRating() == 0
				&& defaultBook.getNumTimesRated() == 0
				&& defaultBook.getAverageRating() == -1.0f);
	}

	/**
	 * Test that several books can be rated
	 *
	 * @throws BookStoreException
	 */

	@Test
	public void testCorrectRatingCalc() throws BookStoreException {
		// Add multiple ratings to the same book
		rateBook(TEST_ISBN, 1);
		rateBook(TEST_ISBN, 2);
		rateBook(TEST_ISBN, 3);
		rateBook(TEST_ISBN, 4);
		rateBook(TEST_ISBN, 5);

		List<StockBook> listBooks = storeManager.getBooks();
		StockBook defaultBook = listBooks.get(0);

		assertTrue(defaultBook.getTotalRating() == 15
				&& defaultBook.getNumTimesRated() == 5
				&& defaultBook.getAverageRating() == 3.0f);
	}

	/**
	 * Tests that all books can be retrieved.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetBooks() throws BookStoreException {
		Set<StockBook> booksAdded = new HashSet<StockBook>();
		booksAdded.add(getDefaultBook());

		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 0, 0, true));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, true));

		booksAdded.addAll(booksToAdd);

		storeManager.addBooks(booksToAdd);

		// Get books in store.
		List<StockBook> listBooks = storeManager.getBooks();

		// Make sure the lists equal each other.
		assertTrue(listBooks.containsAll(booksAdded) && listBooks.size() == booksAdded.size());
	}

	/**
	 * Tests that a list of books with a certain feature can be retrieved.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetCertainBooks() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 0, 0, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false));

		storeManager.addBooks(booksToAdd);

		// Get a list of ISBNs to retrieved.
		Set<Integer> isbnList = new HashSet<Integer>();
		isbnList.add(TEST_ISBN + 1);
		isbnList.add(TEST_ISBN + 2);

		// Get books with that ISBN.
		List<Book> books = client.getBooks(isbnList);

		// Make sure the lists equal each other
		assertTrue(books.containsAll(booksToAdd) && books.size() == booksToAdd.size());
	}

	/**
	 * Tests that books cannot be retrieved if ISBN is invalid.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetInvalidIsbn() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Make an invalid ISBN.
		HashSet<Integer> isbnList = new HashSet<Integer>();
		isbnList.add(TEST_ISBN); // valid
		isbnList.add(-1); // invalid

		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, -1));

		try {
			client.getBooks(isbnList);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Test that top-rated books are retrieved.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetTopRatedBooks() throws BookStoreException {
		addRatedBooks();

		// Get top-rated books
		List<Book> listBooks = client.getTopRatedBooks(2);
		Book topRatedBook = listBooks.get(0);

		assertTrue(listBooks.size() == 2
				&& topRatedBook.getISBN() == TEST_ISBN + 1
				&& topRatedBook.getTitle().equals("The Art of Computer Programming")
				&& topRatedBook.getAuthor().equals("Donald Knuth")
				&& topRatedBook.getPrice() == (float) 300);
	}

	/**
	 * Test that top-rated books cannot be retrieved for a negative number.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */

	@Test
	public void testGetNegativeTopRatedBooks() throws BookStoreException {
		addRatedBooks();

		List<Book> listBooks = new ArrayList<Book>();

		// Try to get top-rated books
		try {
			listBooks = client.getTopRatedBooks(-2);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		assertTrue(listBooks.size() == 0);
	}

	/**
	 * Test that top-rated books are retrieved for a K larger than the number of books in stock.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetLargeKTopRatedBooks() throws BookStoreException {
		addRatedBooks();
//
//		// Get top-rated books
		List<Book> listBooks = client.getTopRatedBooks(5);
		Book topRatedBook = listBooks.get(0);
//		assertTrue(true);
		assertTrue(
				listBooks.size() == 3
//				topRatedBook.getISBN() == TEST_ISBN + 1
//				&& topRatedBook.getTitle().equals("The Art of Computer Programming")
//				&& topRatedBook.getAuthor().equals("Donald Knuth")
//				&& topRatedBook.getPrice() == (float) 300
		);
	}

	/**
	 * Test that top-rated books are retrieved.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetTopRatedBooksDynamic() throws BookStoreException {
		addRatedBooks();

		// Get top-rated books
		List<Book> listBooks = client.getTopRatedBooks(3);
		Book topRatedBook = listBooks.get(0);

		assertTrue(	topRatedBook.getISBN() == TEST_ISBN + 1
				&& topRatedBook.getTitle().equals("The Art of Computer Programming")
				&& topRatedBook.getAuthor().equals("Donald Knuth")
				&& topRatedBook.getPrice() == (float) 300);

		// Set of new ratings
		Set<BookRating> fstRating = new HashSet<BookRating>();
		fstRating.add(new BookRating(TEST_ISBN + 2, 5));
		client.rateBooks(fstRating);
		Set<BookRating> sndRating = new HashSet<BookRating>();
		sndRating.add(new BookRating(TEST_ISBN + 2, 5));
		client.rateBooks(sndRating);

		// Get new top-rated books
		List<Book> newListBooks = client.getTopRatedBooks(2);
		Book newTopRatedBook = newListBooks.get(0);

		assertTrue(	newTopRatedBook.getISBN() == TEST_ISBN + 2
				&& newTopRatedBook.getTitle().equals("The C Programming Language")
				&& newTopRatedBook.getAuthor().equals("Dennis Ritchie and Brian Kerninghan")
				&& newTopRatedBook.getPrice() == (float) 50
		);
	}

	/**
	 * Test editor picks can be retrieved.
	 *
	 * @throws BookStoreException
	 * 			 the book store exception
	 */
	@Test
	public void testGetEditorPicks() throws BookStoreException {
		addRatedBooks();

		// Get editor picks
		List<Book> listBooks = client.getEditorPicks(1);

		assertTrue(listBooks.size() == 1);
	}

	/**
	 * Tests editor picks cannot be retrieved for a negative K.
	 *
	 * @throws BookStoreException
	 * 			 the book store exception
	 */
	@Test
	public void testGetNegativeKEditorPicks() throws BookStoreException {
		addRatedBooks();

		List<Book> listBooks = new ArrayList<Book>();

		// Try to get editor picks
		try {
			listBooks = client.getEditorPicks(-2);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		assertTrue(listBooks.size() == 0);
	}

	/**
	 * Test all editor picks is retrieved for K larger than the number of editor picks.
	 *
	 * @throws BookStoreException
	 * 			 the book store exception
	 */
	@Test
	public void testGetLargeKEditorPicks() throws BookStoreException {
		addRatedBooks();

		// Get editor picks
		List<Book> listBooks = client.getEditorPicks(5);

		assertTrue(listBooks.size() == 2);
	}


	/**
	 * Tear down after class.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws BookStoreException {
		storeManager.removeAllBooks();

		if (!localTest) {
			((BookStoreHTTPProxy) client).stop();
			((StockManagerHTTPProxy) storeManager).stop();
		}
	}
}
