package com.acertainbookstore.client.workloads;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;

/**
 * Helper class to generate stockbooks and isbns modelled similar to Random
 * class
 */
public class BookSetGenerator {
	private int numBooks = 100;
	private int numCopies = 10;
	private int bookTitleLength = 32;
	private int authorNameLength = 32;
	private float editorPickProbability = 0.1f;

	public BookSetGenerator() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Returns num randomly selected isbns from the input set
	 * 
	 * @param num
	 * @return
	 */
	public Set<Integer> sampleFromSetOfISBNs(Set<Integer> isbns, int num) {
		ArrayList<Integer> isbnsAsList = new ArrayList<>(isbns);
		if (isbns.size() == 0) {
			return isbns;
		}

		HashSet<Integer> sampledISBNs = new HashSet<>();
		Random r = new Random();
		for (int n = 0; n < num; n++) {
			Integer i = r.nextInt(isbnsAsList.size());
			sampledISBNs.add(isbnsAsList.get(i));
			isbnsAsList.remove(i);
		}
		return sampledISBNs;
	}

	/**
	 * Generates a random string of targetStringLength length
	 *
	 * @param targetStringLength
	 * @return String
	 */
	public static String randomString(Integer targetStringLength) {
		int leftLimit = 48; // numeral '0'
		int rightLimit = 122; // letter 'z'
		Random random = new Random();

		String generatedString = random.ints(leftLimit, rightLimit + 1)
				.filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
				.limit(targetStringLength)
				.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
				.toString();

		return generatedString;
	}
	/**
	 * Generates true with probability p and false with probability 1-p
	 *
	 * @param p
	 * @return boolean
	 */
	public static boolean randomBooleanWithP(float p) {
		Random random = new Random();
		return random.nextFloat() < p;
	}

	/**
	 * Returns a true with probability p
	 *
	 * @param isbn
	 * @return boolean
	 */
	public ImmutableStockBook generateRandomStockBook(int isbn) {
		ImmutableStockBook book = new ImmutableStockBook(isbn, randomString(bookTitleLength),
				randomString(authorNameLength), 99, numCopies, 0, 0, 0,
				randomBooleanWithP(0.1f));
		return book;
	}

	/**
	 * Return num stock books. For now return an ImmutableStockBook
	 * 
	 * @param num
	 * @return
	 */
	public Set<StockBook> nextSetOfStockBooks(int num) {
		Set<StockBook> stockBooks = new HashSet<>();
		Random r = new Random();
		for (int i = 0; i < num; i++) {
			ImmutableStockBook book = new ImmutableStockBook(r.nextInt(), randomString(bookTitleLength),
					randomString(authorNameLength), 99, numCopies, 0, 0, 0,
					randomBooleanWithP(editorPickProbability));
			stockBooks.add(book);
		}

		return stockBooks;
	}

}
