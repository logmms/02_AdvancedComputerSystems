package com.mybank;

/**
 * {@link AccountException} is a specialized type of exception for accounts.
 * 
 * @see Exception
 */
public class AccountException extends Exception {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1917759675523227483L;

	/**
	 * Instantiates a new {@link AccountException}.
	 */
	public AccountException() {
		super();
	}

	/**
	 * Instantiates a new {@link AccountException}.
	 *
	 * @param message
	 *            the message
	 * @param cause
	 *            the cause
	 * @param enableSuppression
	 *            the enable suppression status
	 * @param writableStackTrace
	 *            the writable stack trace
	 */
	public AccountException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * Instantiates a new {@link AccountException}
	 *
	 * @param message
	 *            the message
	 * @param cause
	 *            the cause
	 */
	public AccountException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Instantiates a new {@link AccountException}.
	 *
	 * @param message
	 *            the message
	 */
	public AccountException(String message) {
		super(message);
	}

	/**
	 * Instantiates a new {@link AccountException}.
	 *
	 * @param cause
	 *            the cause
	 */
	public AccountException(Throwable cause) {
		super(cause);
	}
}
