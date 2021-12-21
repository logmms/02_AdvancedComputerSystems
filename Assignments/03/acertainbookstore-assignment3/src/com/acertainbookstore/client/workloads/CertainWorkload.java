/**
 * 
 */
package com.acertainbookstore.client.workloads;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;

/**
 * 
 * CertainWorkload class runs the workloads by different workers concurrently.
 * It configures the environment for the workers using WorkloadConfiguration
 * objects and reports the metrics
 * 
 */
public class CertainWorkload {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("threads, throughput, latency");
		for (int i = 1; i <= 128; i++) {
			System.out.print(i + ", ");
			Thread.sleep(100);
			tits(i);
		}
	}

	private static void tits(int numConcurrentWorkloadThreads) throws Exception {
		//int numConcurrentWorkloadThreads = 16;
		String serverAddress = "http://localhost:8081";
		boolean localTest = false;
		List<WorkerRunResult> workerRunResults = new ArrayList<WorkerRunResult>();
		List<Future<WorkerRunResult>> runResults = new ArrayList<Future<WorkerRunResult>>();

		// Initialize the RPC interfaces if its not a localTest, the variable is
		// overriden if the property is set
		String localTestProperty = System
				.getProperty(BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
		localTest = (localTestProperty != null) ? Boolean
				.parseBoolean(localTestProperty) : localTest;

		BookStore bookStore = null;
		StockManager stockManager = null;
		if (localTest) {
			CertainBookStore store = new CertainBookStore();
			bookStore = store;
			stockManager = store;
		} else {
			stockManager = new StockManagerHTTPProxy(serverAddress + "/stock");
			bookStore = new BookStoreHTTPProxy(serverAddress);
		}

		// Generate data in the bookstore before running the workload
		initializeBookStoreData(bookStore, stockManager);

		ExecutorService exec = Executors
				.newFixedThreadPool(numConcurrentWorkloadThreads);

		for (int i = 0; i < numConcurrentWorkloadThreads; i++) {
			WorkloadConfiguration config = new WorkloadConfiguration(bookStore, stockManager);
			Worker workerTask = new Worker(config);
			// Keep the futures to wait for the result from the thread
			runResults.add(exec.submit(workerTask));
		}

		// Get the results from the threads using the futures returned
		for (Future<WorkerRunResult> futureRunResult : runResults) {
			WorkerRunResult runResult = futureRunResult.get(); // blocking call
			workerRunResults.add(runResult);
		}

		exec.shutdownNow(); // shutdown the executor

		// Finished initialization, stop the clients if not localTest
		if (!localTest) {
			((BookStoreHTTPProxy) bookStore).stop();
			((StockManagerHTTPProxy) stockManager).stop();
		}

		reportMetric(workerRunResults);
	}

	/**
	 * Computes the metrics and prints them
	 * 
	 * @param workerRunResults
	 */
	public static void reportMetric(List<WorkerRunResult> workerRunResults) {
		int totalFrequentBookStoreInteractionRuns = 0;
		int successfulFrequentBookStoreInteractionRuns = 0;
		int totalRuns = 0;
		// Latency in miliseconds
		int totalLatency = 0;
		float goodput = 0;
		float throughput = 0;

		for (WorkerRunResult result : workerRunResults) {
			totalFrequentBookStoreInteractionRuns += result.getTotalFrequentBookStoreInteractionRuns();
			successfulFrequentBookStoreInteractionRuns += result.getSuccessfulFrequentBookStoreInteractionRuns();
			totalRuns += result.getTotalRuns();
			goodput += (float) result.getSuccessfulFrequentBookStoreInteractionRuns() / (float)((float) result.getElapsedTimeInNanoSecs() / 1000000);
			throughput += (float) result.getTotalFrequentBookStoreInteractionRuns() / ((float) result.getElapsedTimeInNanoSecs() / 1000000);
			totalLatency += result.getElapsedTimeInNanoSecs() / 100000;
		}

		float failureRate = 1 - goodput / throughput;
		if (failureRate > 0.1) {
			System.out.println("Failure rate too high");
		}

		float customerInteractionRate = (float) totalFrequentBookStoreInteractionRuns / totalRuns;
		if (0.55 > customerInteractionRate || customerInteractionRate > 0.65) {
			System.out.println("Imbalanced customer interactions");
		}

		float avgLatency = (float) totalLatency / successfulFrequentBookStoreInteractionRuns;

		System.out.println(throughput + ", " + avgLatency);
		// TODO: You should aggregate metrics and output them for plotting here
	}


	/**
	 * Generate the data in bookstore before the workload interactions are run
	 * 
	 * Ignores the serverAddress if its a localTest
	 * 
	 */
	public static void initializeBookStoreData(BookStore bookStore, StockManager stockManager) throws BookStoreException {
		Integer a = 2;
		// TODO: You should initialize data for your bookstore here

	}
}
