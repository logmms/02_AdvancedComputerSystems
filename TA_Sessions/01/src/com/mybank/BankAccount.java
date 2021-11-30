package com.mybank;

/**
 * {@link BankAccount} models a bank {@link Account}.
 * 
 * @see Account
 */
public class BankAccount implements Account {

	/** The balance of the account. */
	private int balance;

	/** The CPR. */
	private int cpr;

	/**
	 * Instantiates a new {@link BankAccount}.
	 *
	 * @param newCpr
	 *            the new CPR
	 */
	public BankAccount(int newCpr) {
		cpr = newCpr;
		balance = 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mybank.BankAccount#getBalance()
	 */
	public synchronized int getBalance() {
		return balance;
	}

	/**
	 * Adds n units of currency to the balance.
	 *
	 * @param n
	 *            the number of units of currency
	 * @throws AccountException
	 *             (if n < 0)
	 */
	public synchronized void deposit(int n) throws AccountException {
		if (n < 0) {
			throw new AccountException("n less than 0");
		}

		balance = balance + n;
	}

	/**
	 * Subtracts n units of currency from the balance.
	 *
	 * @param n
	 *            the number of units of currency
	 * @throws AccountException
	 *             (if n < 0)
	 */
	public synchronized void withdraw(int n) throws AccountException {
		if (n < 0) {
			throw new AccountException("n less than 0");
		}

		if (n > balance) {
			throw new AccountException("n more than account balance");
		}

		balance = balance - n;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Bank account of " + cpr;
	}
}
