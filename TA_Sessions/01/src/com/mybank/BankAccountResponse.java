package com.mybank;

/**
 * {@link BankAccountResponse} is the data structure that encapsulates a HTTP
 * response from the bank server to the client. The data structure contains
 * error messages from the server if an error occurred.
 */
class BankAccountResponse {

	/** The exception. */
	private AccountException exception;

	/** The object. */
	private Object obj;

	/**
	 * Instantiates a new {@link BankAccountResponse}.
	 *
	 * @param exception
	 *            the exception
	 * @param obj
	 *            the parameter
	 */
	public BankAccountResponse(AccountException exception, Object obj) {
		this.setException(exception);
		this.setObject(obj);
	}

	/**
	 * Instantiates a new {@link BankAccountResponse}.
	 */
	public BankAccountResponse() {
		this.setException(null);
		this.setObject(null);
	}

	/**
	 * Gets the exception.
	 *
	 * @return the exception
	 */
	public AccountException getException() {
		return exception;
	}

	/**
	 * Sets the exception.
	 *
	 * @param exception
	 *            the new exception
	 */
	public void setException(AccountException exception) {
		this.exception = exception;
	}

	/**
	 * Gets the object.
	 *
	 * @return the object
	 */
	public Object getObject() {
		return obj;
	}

	/**
	 * Sets the object.
	 *
	 * @param obj
	 *            the new object
	 */
	public void setObject(Object obj) {
		this.obj = obj;
	}
}
