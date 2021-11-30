package com.mybank;

/**
 * {@link MessageTag} enumerates the available message tags on the server API
 * that the clients must adhere to.
 */
public enum MessageTag {

	/** The get balance message tag. */
	GETBALANCE,

	/** The deposit message tag. */
	DEPOSIT,

	/** The withdraw message tag. */
	WITHDRAW;
}
