package com.mybank;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 * {@link BankAccountHTTPProxy} implements the {@link Account}.
 * 
 * @see Account
 */
public class BankAccountHTTPProxy implements Account {

	/** The client. */
	protected HttpClient client;

	/** The server address. */
	protected String serverAddress;

	/** The serializer. */
	private static ThreadLocal<BankAccountSerializer> serializer;

	/** The Constant strERR_CLIENT_REQUEST_SENDING. */
	public static final String STR_ERR_CLIENT_REQUEST_SENDING = "ERR_CLIENT_REQUEST_SENDING";

	/** The Constant strERR_CLIENT_REQUEST_EXCEPTION. */
	public static final String STR_ERR_CLIENT_REQUEST_EXCEPTION = "ERR_CLIENT_REQUEST_EXCEPTION";

	/** The Constant strERR_CLIENT_REQUEST_TIMEOUT. */
	public static final String STR_ERR_CLIENT_REQUEST_TIMEOUT = "CLIENT_REQUEST_TIMEOUT";

	/** The Constant strERR_CLIENT_RESPONSE_DECODING. */
	public static final String STR_ERR_CLIENT_RESPONSE_DECODING = "CLIENT_RESPONSE_DECODING";

	/** The Constant strERR_CLIENT_UNKNOWN. */
	public static final String STR_ERR_CLIENT_UNKNOWN = "CLIENT_UNKNOWN";

	/** The Constant strERR_CLIENT_ENCODING. */
	public static final String STR_ERR_CLIENT_ENCODING = "CLIENT_ENCODING";

	/** The Constant CLIENT_MAX_CONNECTION_ADDRESS. */
	public static final int CLIENT_MAX_CONNECTION_ADDRESS = 200;

	/** The Constant CLIENT_MAX_THREADSPOOL_THREADS. */
	public static final int CLIENT_MAX_THREADSPOOL_THREADS = 250;

	/** The Constant CLIENT_MAX_TIMEOUT_MILLISECS. */
	public static final int CLIENT_MAX_TIMEOUT_MILLISECS = 30000;

	/**
	 * Initializes a new {@link BankAccountHTTPProxy}.
	 *
	 * @param serverAddress
	 *            the server address
	 * @throws Exception
	 *             the exception thrown in case of failed initialization
	 */
	public BankAccountHTTPProxy(String serverAddress) throws Exception {

		// Setup the serializer.
		serializer = ThreadLocal.withInitial(BankAccountXStreamSerializer::new);

		setServerAddress(serverAddress);
		client = new HttpClient();

		// Max concurrent connections to every address.
		client.setMaxConnectionsPerDestination(CLIENT_MAX_CONNECTION_ADDRESS);

		// Max number of threads.
		client.setExecutor(new QueuedThreadPool(CLIENT_MAX_THREADSPOOL_THREADS));

		// Seconds timeout; if no server reply, the request expires.
		client.setConnectTimeout(CLIENT_MAX_TIMEOUT_MILLISECS);

		client.start();
	}

	/**
	 * Gets the server address.
	 *
	 * @return the server address
	 */
	public String getServerAddress() {
		return serverAddress;
	}

	/**
	 * Sets the server address.
	 *
	 * @param serverAddress
	 *            the new server address
	 */
	public void setServerAddress(String serverAddress) {
		this.serverAddress = serverAddress;
	}

	/**
	 * Stop.
	 *
	 * @throws Exception
	 *             the exception
	 */
	public void stop() throws Exception {
		client.stop();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mybank.Account#getBalance()
	 */
	public int getBalance() throws AccountException {
		String urlString = serverAddress + "/" + MessageTag.GETBALANCE;
		BankAccountRequest bankAccountRequest = BankAccountRequest.newGetRequest(urlString);
		BankAccountResponse bankAccountResponse = BankAccountUtility.performHttpExchange(client, bankAccountRequest,
				serializer.get());
		return (Integer) bankAccountResponse.getObject();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mybank.Account#withdraw(int)
	 */
	public void withdraw(int n) throws AccountException {
		String urlString = serverAddress + "/" + MessageTag.WITHDRAW;
		Integer integerN = n;
		BankAccountRequest bankAccountRequest = BankAccountRequest.newPostRequest(urlString, integerN);
		BankAccountUtility.performHttpExchange(client, bankAccountRequest, serializer.get());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mybank.Account#deposit(int)
	 */
	public void deposit(int n) throws AccountException {
		// Write your code here.
	}
}
