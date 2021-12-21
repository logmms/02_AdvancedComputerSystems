package com.acertainbookstore.client.workloads;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import com.acertainbookstore.business.StockBook;

/**
 * Helper class to generate stockbooks and isbns modelled similar to Random
 * class
 */
public class BookSetGenerator {

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
		if (isbns.size() == 0) {
			return isbns;
		}
		ArrayList<Integer> isbnsAsList = new ArrayList<>(isbns);
		HashSet<Integer> sampledISBNs = new HashSet<>();
		Random r = new Random();
		for (int n = 0; n < num; n++) {
			Integer i = r.nextInt() * isbnsAsList.size();
			sampledISBNs.add(isbnsAsList.get(i));
			isbnsAsList.remove(i);
		}
		return sampledISBNs;
	}

	/**
	 * Return num stock books. For now return an ImmutableStockBook
	 * 
	 * @param num
	 * @return
	 */
	public Set<StockBook> nextSetOfStockBooks(int num) {
		return new HashSet<StockBook>();
	}

}
