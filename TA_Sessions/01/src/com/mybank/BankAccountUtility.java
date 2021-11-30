package com.mybank;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentProvider;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.http.HttpMethod;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

/**
 * {@link BankAccountUtility} implements utility methods used by the servers and
 * clients.
 */
public final class BankAccountUtility {

	/** The Constant STR_ERR_CLIENT_REQUEST_SENDING. */
	public static final String STR_ERR_CLIENT_REQUEST_SENDING = "ERR_CLIENT_REQUEST_SENDING";

	/** The Constant STR_ERR_CLIENT_REQUEST_EXCEPTION. */
	public static final String STR_ERR_CLIENT_REQUEST_EXCEPTION = "ERR_CLIENT_REQUEST_EXCEPTION";

	/** The Constant STR_ERR_CLIENT_REQUEST_TIMEOUT. */
	public static final String STR_ERR_CLIENT_REQUEST_TIMEOUT = "CLIENT_REQUEST_TIMEOUT";

	/** The Constant STR_ERR_CLIENT_RESPONSE_DECODING. */
	public static final String STR_ERR_CLIENT_RESPONSE_DECODING = "CLIENT_RESPONSE_DECODING";

	/** The Constant STR_ERR_CLIENT_UNKNOWN. */
	public static final String STR_ERR_CLIENT_UNKNOWN = "CLIENT_UNKNOWN";

	/** The Constant STR_ERR_CLIENT_ENCODING. */
	public static final String STR_ERR_CLIENT_ENCODING = "CLIENT_ENCODING";

	/** The Constant XmlStreams. */
	// We use pooling because creating an XStream object is expensive.
	public static final ThreadLocal<XStream> XML_STREAMS = new ThreadLocal<XStream>() {

		@Override
		protected XStream initialValue() {
			return new XStream(new StaxDriver());
		}
	};

	/**
	 * Prevents instantiation of a new {@link BankAccountUtility}.
	 */
	private BankAccountUtility() {
		// Prevents instantiation.
	}

	/**
	 * Converts a string to a float if possible else it returns the signal value
	 * for failure passed as parameter.
	 *
	 * @param str
	 *            the string
	 * @param failureSignal
	 *            the failure signal
	 * @return the float
	 */
	public static float convertStringToFloat(String str, float failureSignal) {
		float returnValue = failureSignal;
		try {
			returnValue = Float.parseFloat(str);

		} catch (NumberFormatException | NullPointerException ex) {
			// Do nothing.
		}

		return returnValue;
	}

	/**
	 * Converts a string to an integer if possible else it returns the signal
	 * value for failure passed as parameter.
	 *
	 * @param str
	 *            the string
	 * @return the integer
	 * @throws Exception
	 *             the exception
	 */
	public static int convertStringToInt(String str) throws Exception {
		int returnValue = 0;
		try {
			returnValue = Integer.parseInt(str);
		} catch (Exception ex) {
			throw new Exception(ex);
		}
		return returnValue;
	}

	/**
	 * Convert a request URI to the message tags supported in CertainBookStore.
	 *
	 * @param requestURI
	 *            the request URI
	 * @return the message tag
	 */
	public static MessageTag convertURItoMessageTag(String requestURI) {

		try {
			return MessageTag.valueOf(requestURI.substring(1).toUpperCase());
		} catch (IllegalArgumentException | NullPointerException ex) {
			// Enum type matching failed so non supported message or the request
			// URI was empty.
		}

		return null;
	}

	/**
	 * Checks if is empty.
	 *
	 * @param str
	 *            the string
	 * @return true, if is empty
	 */
	public static boolean isEmpty(String str) {
		return str == null || str.isEmpty();
	}

	/**
	 * Perform HTTP exchange.
	 *
	 * @param client
	 *            the client
	 * @param bankAccountRequest
	 *            the book store request
	 * @param serializer
	 *            the serializer
	 * @return the book store response
	 * @throws BookStoreException
	 *             the book store exception
	 */
	public static BankAccountResponse performHttpExchange(HttpClient client, BankAccountRequest bankAccountRequest,
			BankAccountSerializer serializer) throws AccountException {
		Request request;

		switch (bankAccountRequest.getMethod()) {
		case GET:
			request = client.newRequest(bankAccountRequest.getURLString()).method(HttpMethod.GET);
			break;

		case POST:
			try {
				byte[] serializedValue = serializer.serialize(bankAccountRequest.getInputValue());
				ContentProvider contentProvider = new BytesContentProvider(serializedValue);
				request = client.POST(bankAccountRequest.getURLString()).content(contentProvider);
			} catch (IOException ex) {
				throw new AccountException("Serialization error", ex);
			}

			break;

		default:
			throw new IllegalArgumentException("HTTP Method not supported.");
		}

		ContentResponse response;

		try {
			response = request.send();
		} catch (InterruptedException ex) {
			throw new AccountException(STR_ERR_CLIENT_REQUEST_SENDING, ex);
		} catch (TimeoutException ex) {
			throw new AccountException(STR_ERR_CLIENT_REQUEST_TIMEOUT, ex);
		} catch (ExecutionException ex) {
			throw new AccountException(STR_ERR_CLIENT_REQUEST_EXCEPTION, ex);
		}

		BankAccountResponse bankAccountResponse;

		try {
			bankAccountResponse = (BankAccountResponse) serializer.deserialize(response.getContent());
		} catch (IOException ex) {
			throw new AccountException("Deserialization error", ex);
		}

		AccountException exception = bankAccountResponse.getException();

		if (exception != null) {
			throw exception;
		}

		return bankAccountResponse;
	}
}
