package com.mybank;

import java.io.IOException;
import java.io.Reader;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

/**
 * {@link BankAccountHTTPMessageHandler} handles the messages on the
 * {@link Account} through HTTP.
 * 
 * @see AbstractHandler
 */
public class BankAccountHTTPMessageHandler extends AbstractHandler {

	/** The serializer. */
	private static ThreadLocal<BankAccountSerializer> serializer;

	/** The account. */
	private final Account account;

	/**
	 * Instantiates a new {@link BankAccountHTTPMessageHandler}.
	 *
	 * @param account
	 *            the account
	 */
	public BankAccountHTTPMessageHandler(Account account) {
		this.account = account;
		serializer = ThreadLocal.withInitial(BankAccountXStreamSerializer::new);
	}

	/**
	 * Handles the message from the client.
	 *
	 * @param target
	 *            the target
	 * @param baseRequest
	 *            the base request
	 * @param request
	 *            the request
	 * @param response
	 *            the response
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws ServletException
	 *             the servlet exception
	 */
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		MessageTag messageTag = null;
		String requestURI;

		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		requestURI = request.getRequestURI();

		// Need to do request multiplexing
		if (!BankAccountUtility.isEmpty(requestURI)) {
			messageTag = BankAccountUtility.convertURItoMessageTag(requestURI);
		}

		// the RequestURI before the switch
		if (messageTag == null) {
			System.err.println("Unknown message tag");
		} else {

			switch (messageTag) {
			case GETBALANCE:
				getBalance(response);
				break;

			case DEPOSIT:
				// Write your implementation here!
				break;

			case WITHDRAW:
				withdraw(request, response);
				break;

			default:
				System.err.println("Unhandled message tag");
				break;
			}
		}

		// Mark the request as handled so that the HTTP response can be sent
		baseRequest.setHandled(true);
	}

	/**
	 * Withdraws from the account.
	 *
	 * @param request
	 *            the request
	 * @param response
	 *            the response
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private void withdraw(HttpServletRequest request, HttpServletResponse response) throws IOException {
		byte[] serializedRequestContent = getSerializedRequestContent(request);
		Integer n = (Integer) serializer.get().deserialize(serializedRequestContent);

		BankAccountResponse bankAccountResponse = new BankAccountResponse();

		try {
			account.withdraw(n);
		} catch (Exception ex) {
			bankAccountResponse.setException(new AccountException("Could not withdraw.", ex));
		}

		byte[] serializedResponseContent = serializer.get().serialize(bankAccountResponse);
		response.getOutputStream().write(serializedResponseContent);
	}

	/**
	 * Gets the balance from the account.
	 *
	 * @param response
	 *            the response
	 * @return the balance
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private void getBalance(HttpServletResponse response) throws IOException {
		BankAccountResponse bankAccountResponse = new BankAccountResponse();

		try {
			Integer balance = account.getBalance();
			bankAccountResponse.setObject(balance);
		} catch (Exception ex) {
			bankAccountResponse.setException(new AccountException("Could not get the balance.", ex));
		}

		byte[] serializedResponseContent = serializer.get().serialize(bankAccountResponse);
		response.getOutputStream().write(serializedResponseContent);
	}

	/**
	 * Gets the serialized request content.
	 *
	 * @param request
	 *            the request
	 * @return the serialized request content
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private static byte[] getSerializedRequestContent(HttpServletRequest request) throws IOException {
		Reader reader = request.getReader();
		int len = request.getContentLength();
		int bytesCopied = 0;

		// Request must be read into a char[] first
		char[] res = new char[len];

		while (bytesCopied < len) {
			int bytesRead = reader.read(res, bytesCopied, len - bytesCopied);

			if (bytesRead < 0) {
				throw new IOException("Could not read the entire data.");
			}

			bytesCopied += bytesRead;
		}

		reader.close();
		return new String(res).getBytes();
	}
}
