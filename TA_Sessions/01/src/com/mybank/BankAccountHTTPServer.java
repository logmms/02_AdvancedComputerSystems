package com.mybank;

/**
 * {@link BankAccountHTTPServer} starts the bookstore HTTP server that the
 * clients will communicate with.
 */
public class BankAccountHTTPServer {
	
	/** The Constant DEFAULT_PORT */
	private static final int DEFAULT_PORT = 8081;

	/** The Constant PROPERTY_KEY_SERVER_PORT. */
	public static final String PROPERTY_KEY_SERVER_PORT = "port";

	/**
	 * Prevents instantiation of a new {@link BankAccountHTTPServer}.
	 */
	private BankAccountHTTPServer() {
		// Prevent instantiation.
	}

	/**
	 * The main method.
	 *
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args) {
		Account newba = new BankAccount(123456789);
		int listenOnPort = DEFAULT_PORT;
		BankAccountHTTPMessageHandler handler = new BankAccountHTTPMessageHandler(newba);
		String serverPortString = System.getProperty(PROPERTY_KEY_SERVER_PORT);

		if (serverPortString != null) {
			try {
				listenOnPort = Integer.parseInt(serverPortString);
			} catch (NumberFormatException ex) {
				System.err.println(ex);
			}
		}

		if (BankAccountHTTPServerUtility.createServer(listenOnPort, handler)) {
			// Do nothing.
		}
	}
}
