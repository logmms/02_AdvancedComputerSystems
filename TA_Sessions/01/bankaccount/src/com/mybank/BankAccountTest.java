package com.mybank;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * {@link BankAccountTest} tests the {@link Account} interface.
 * 
 * @see Account
 */
public class BankAccountTest {

	/** The Constant TEST_CPR. */
	private static final int TEST_CPR = 1234567890;

	/** The local test. */
	private static boolean localTest = false;

	/** The client. */
	private static Account client;

	/**
	 * Sets up before class.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		if (localTest) {
			client = new BankAccount(TEST_CPR);
		} else {
			client = new BankAccountHTTPProxy("http://localhost:8081");
		}
	}

	/**
	 * Test balance zero.
	 * 
	 * @throws AccountException
	 *             if the balance cannot be obtained
	 */
	@Test
	public void testBalanceZero() throws AccountException {
		int balance = client.getBalance();
		assertTrue(balance == 0);
	}

	/**
	 * Test correct balance.
	 */
	@Test
	public void testCorrectBalance() throws AccountException {
		// Tests balance after series of deposit-withdraw invocations
		// fail("Not implemented");
		client.deposit(50);
		int balance = client.getBalance();
		assertTrue(balance == 50);
	}

	/**
	 * Test over draft.
	 */
	@Test
	public void testOverDraft() {

		// BankAccountException should be thrown if we try to withdraw amount
		// grater than balance
		fail("Not implemented");
	}

	/**
	 * Test negative withdraw.
	 */
	@Test
	public void testNegativeWithdraw() {

		// BankAccountException should be thrown if we try to withdraw a
		// negative amount.
		fail("Not implemented");
	}

	/**
	 * Test negative deposit.
	 */
	@Test
	public void testNegativeDeposit() {

		// BankAccountException should be thrown if we try to deposit a negative
		// amount.
		fail("Not implemented");
	}

	/**
	 * Tear down after class.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		if (!localTest) {
			((BankAccountHTTPProxy) client).stop();
		}
	}
}
