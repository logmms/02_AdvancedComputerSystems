package com.mybank;

import org.eclipse.jetty.http.HttpMethod;

/**
 * {@link BankAccountRequest} is the data structure that encapsulates a HTTP
 * request from the bank client to the server.
 */
public final class BankAccountRequest {

	/** The method. */
	private final HttpMethod method;

	/** The URL string. */
	private final String urlString;

	/** The input value. */
	private final Object inputValue;

	/**
	 * Instantiates a new {@link BankAccountRequest}.
	 *
	 * @param method
	 *            the method
	 * @param urlString
	 *            the URL string
	 * @param inputValue
	 *            the input value
	 */
	private BankAccountRequest(HttpMethod method, String urlString, Object inputValue) {
		this.method = method;
		this.urlString = urlString;
		this.inputValue = inputValue;
	}

	/**
	 * Gets the method.
	 *
	 * @return the method
	 */
	public HttpMethod getMethod() {
		return method;
	}

	/**
	 * Gets the URL string.
	 *
	 * @return the URL string
	 */
	public String getURLString() {
		return urlString;
	}

	/**
	 * Gets the input value.
	 *
	 * @return the input value
	 */
	public Object getInputValue() {
		return inputValue;
	}

	/**
	 * Gets a new GET request.
	 *
	 * @param urlString
	 *            the URL string
	 * @return the book store request
	 */
	public static BankAccountRequest newGetRequest(String urlString) {
		return new BankAccountRequest(HttpMethod.GET, urlString, null);
	}

	/**
	 * Gets a new POST request.
	 *
	 * @param urlString
	 *            the URL string
	 * @param inputValue
	 *            the input value
	 * @return the book store request
	 */
	public static BankAccountRequest newPostRequest(String urlString, Object inputValue) {
		return new BankAccountRequest(HttpMethod.POST, urlString, inputValue);
	}
}
