package com.acertainbookstore.client.tests;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.SingleLockConcurrentCertainBookStore;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.business.TwoLevelLockingConcurrentCertainBookStore;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;

/**
 * {@link BookStoreTest} tests the {@link BookStore} interface.
 *
 * @see BookStore
 */
public class ConcurrencyTest {

    /** The Constant TEST_ISBN. */
    private static final int TEST_ISBN = 3044560;

    /** The Constant NUM_COPIES. */
    private static final int NUM_COPIES = 100;

    /** The local test. */
    private static boolean localTest = true;

    /** Single lock test */
    private static boolean singleLock = false;

    /** The store manager. */
    private static StockManager storeManager;

    /** The client. */
    private static BookStore client;

    /**
     * Sets the up before class.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        try {
            String localTestProperty = System.getProperty(BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
            localTest = (localTestProperty != null) ? Boolean.parseBoolean(localTestProperty) : localTest;

            String singleLockProperty = System.getProperty(BookStoreConstants.PROPERTY_KEY_SINGLE_LOCK);
            singleLock = (singleLockProperty != null) ? Boolean.parseBoolean(singleLockProperty) : singleLock;

            if (localTest) {
                if (singleLock) {
                    SingleLockConcurrentCertainBookStore store = new SingleLockConcurrentCertainBookStore();
                    storeManager = store;
                    client = store;
                } else {
                    TwoLevelLockingConcurrentCertainBookStore store = new TwoLevelLockingConcurrentCertainBookStore();
                    storeManager = store;
                    client = store;
                }
            } else {
                storeManager = new StockManagerHTTPProxy("http://localhost:8081/stock");
                client = new BookStoreHTTPProxy("http://localhost:8081");
            }

            storeManager.removeAllBooks();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method to add some books.
     *
     * @param isbn
     *            the isbn
     * @param copies
     *            the copies
     * @throws BookStoreException
     *             the book store exception
     */
    public void addBooks(int isbn, int copies) throws BookStoreException {
        Set<StockBook> booksToAdd = new HashSet<StockBook>();
        StockBook book = new ImmutableStockBook(isbn, "Test of Thrones", "George RR Testin'", (float) 10, copies, 0, 0,
                0, false);
        booksToAdd.add(book);
        storeManager.addBooks(booksToAdd);
    }

    /**
     * Helper method to get the default book used by initializeBooks.
     *
     * @return the default book
     */
    public StockBook getDefaultBook() {
        return new ImmutableStockBook(TEST_ISBN, "Harry Potter and JUnit", "JK Unit", (float) 10, NUM_COPIES, 0, 0, 0,
                false);
    }

    public void runClients(Runnable c1, Runnable c2) throws InterruptedException {
        Thread t1 = new Thread(c1);
        t1.setName("Client 1");

        Thread t2 = new Thread(c2);
        t2.setName("Client 2");

        t1.start();
        t2.start();

        t1.join();
        t2.join();
    }

    /**
     * Method to add a book, executed before every test case is run.
     *
     * @throws BookStoreException
     *             the book store exception
     */
    @Before
    public void initializeBooks() throws BookStoreException {
        Set<StockBook> booksToAdd = new HashSet<StockBook>();
        booksToAdd.add(getDefaultBook());
        storeManager.addBooks(booksToAdd);
    }

    /**
     * Method to clean up the book store, execute after every test case is run.
     *
     * @throws BookStoreException
     *             the book store exception
     */
    @After
    public void cleanupBooks() throws BookStoreException {
        storeManager.removeAllBooks();
    }

    /**
     * Test concurrent add book copies and buy books.
     *
     * @throws BookStoreException
     *             the book store exception
     */
    @Test
    public void testConcurrentAddCopiesAndBuy() throws InterruptedException, BookStoreException {
        int numCalls = 100;
        Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
        booksToBuy.add(new BookCopy(TEST_ISBN, 1));

        Set<BookCopy> booksToAdd = new HashSet<BookCopy>();
        booksToAdd.add(new BookCopy(TEST_ISBN, 1));

        class Test1C1 implements Runnable
        {
            public void run()
            {
                for (int i = 0; i < numCalls; i++) {
                    try {
//                        System.out.println(Thread.currentThread().getName() + " Buying book");
                        client.buyBooks(booksToBuy);
                    } catch (BookStoreException e) {
                        ;
                    }
                }

            }
        }

        class Test1C2 implements Runnable
        {
            public void run()
            {
                for (int i = 0; i < numCalls; i++) {
                    try {
//                        System.out.println(Thread.currentThread().getName() + " Adding copies");
                        storeManager.addCopies(booksToAdd);
                    } catch (BookStoreException e) {
                        ;
                    }
                }

            }
        }

        runClients(new Test1C1(), new Test1C2());
        List<StockBook> stockBooks = storeManager.getBooks();
        assertEquals(1, stockBooks.size());
        assertEquals(numCalls, stockBooks.get(0).getNumCopies());
    }


    private static boolean invalid = false;

    /**
     * Test concurrent add book copies, buy book copies and get books.
     *
     * @throws BookStoreException
     *             the book store exception
     */
    @Test
    public void testConcurrentAddCopiesBuyRead() throws InterruptedException, BookStoreException {
        int numCalls = 100;
        Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
        booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES));

        Set<BookCopy> booksToAdd = new HashSet<BookCopy>();
        booksToAdd.add(new BookCopy(TEST_ISBN, NUM_COPIES));

        class Test2C1 implements Runnable
        {
            public void run()
            {
                for (int i = 0; i < numCalls; i++) {
                    try {
                        client.buyBooks(booksToBuy);
                        storeManager.addCopies(booksToAdd);
                    } catch (BookStoreException e) {
                        ;
                    }
                }

            }
        }

        class Test2C2 implements Runnable
        {
            public void run()
            {
                for (int i = 0; i < numCalls; i++) {
                    try {
                        List<StockBook> stockBooks = storeManager.getBooks();

                        if(numCalls != stockBooks.get(0).getNumCopies() && 0 != stockBooks.get(0).getNumCopies()) {
                            invalid = true;
                        }
                    } catch (BookStoreException e) {
                        ;
                    }
                }

            }
        }

        runClients(new Test2C1(), new Test2C2());
        assertFalse(invalid);
    }


    /**
     * Tear down after class.
     *
     * @throws BookStoreException
     *             the book store exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws BookStoreException {
        storeManager.removeAllBooks();

        if (!localTest) {
            ((BookStoreHTTPProxy) client).stop();
            ((StockManagerHTTPProxy) storeManager).stop();
        }
    }
}
