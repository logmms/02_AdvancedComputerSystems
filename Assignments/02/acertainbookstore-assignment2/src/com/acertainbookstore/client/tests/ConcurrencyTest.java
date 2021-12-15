package com.acertainbookstore.client.tests;

import static org.junit.Assert.*;

import java.util.*;
import java.util.function.Function;
import java.util.concurrent.ThreadLocalRandom;

import com.acertainbookstore.business.*;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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

    public void runClients(List<Runnable> clients) throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        for (Runnable client : clients) {
            Thread t = new Thread(client);
            threads.add(t);
        }

        for (Thread t : threads) {
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }
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
     * TEST 1
     * Test concurrent add book copies and buy books. Checks that WW conflicts does not occur
     *
     * @throws BookStoreException
     *             the book store exception
     */
    @Test
    public void testConcurrentAddCopiesAndBuy() throws InterruptedException, BookStoreException {
        Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
        booksToBuy.add(new BookCopy(TEST_ISBN, 1));

        Set<BookCopy> booksToAdd = new HashSet<BookCopy>();
        booksToAdd.add(new BookCopy(TEST_ISBN, 1));


        class Test1C1 implements Runnable
        {
            public void run()
            {
                for (int i = 0; i < NUM_COPIES; i++) {
                    try {
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
                for (int i = 0; i < NUM_COPIES; i++) {
                    try {
                        storeManager.addCopies(booksToAdd);
                    } catch (BookStoreException e) {
                        ;
                    }
                }

            }
        }

        runClients(List.of(new Test1C1(), new Test1C2()));
        List<StockBook> stockBooks = storeManager.getBooks();
        assertEquals(1, stockBooks.size());
        assertEquals(NUM_COPIES, stockBooks.get(0).getNumCopies());
    }

    private static boolean invalid = false;

    /**
     * TEST 2
     * Test concurrent add book copies, buy book copies and get books.
     *
     * @throws BookStoreException
     *             the book store exception
     */
    @Test
    public void testConcurrentAddCopiesBuyRead() throws InterruptedException, BookStoreException {
        addBooks(1337, NUM_COPIES);

        Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
        booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES));
        booksToBuy.add(new BookCopy(1337, NUM_COPIES));

        Set<BookCopy> booksToAdd = new HashSet<BookCopy>();
        booksToAdd.add(new BookCopy(TEST_ISBN, NUM_COPIES));
        booksToAdd.add(new BookCopy(1337, NUM_COPIES));

        class Test2C1 implements Runnable
        {
            public void run()
            {
                for (int i = 0; i < NUM_COPIES; i++) {
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
                for (int i = 0; i < NUM_COPIES; i++) {
                    try {
                        List<StockBook> stockBooks = storeManager.getBooks();

                        if(NUM_COPIES != stockBooks.get(0).getNumCopies()
                                && NUM_COPIES != stockBooks.get(1).getNumCopies()
                                && 0 != stockBooks.get(0).getNumCopies()
                                && 0 != stockBooks.get(1).getNumCopies()) {
                            invalid = true;
                        }
                    } catch (BookStoreException e) {
                        ;
                    }
                }

            }
        }

        runClients(List.of(new Test2C1(), new Test2C2()));
        assertFalse(invalid);
    }


    /**
     * TEST 3
     * Test reading Uncommitted Data (WR Conflicts, “dirty reads”)
     * T1: R(A), W(A),                    R(B), W(B), Abort
     * T2:                R(A), W(A), C
     * Abort is simulated with adding copies an invalid isbn
     * @throws BookStoreException
     *             the book store exception
     */
    @Test
    public void testDirtyReads() throws InterruptedException, BookStoreException {
        Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
        booksToBuy.add(new BookCopy(TEST_ISBN, 1));

        Set<BookCopy> booksToAdd = new HashSet<BookCopy>();
        booksToAdd.add(new BookCopy(TEST_ISBN, 1));
        booksToAdd.add(new BookCopy(1337, 1));

        class Test1C1 implements Runnable
        {
            public void run()
            {
                for (int i = 0; i < NUM_COPIES; i++) {
                    try {
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
                for (int i = 0; i < NUM_COPIES; i++) {
                    try {
                        storeManager.addCopies(booksToAdd);
                    } catch (BookStoreException e) {
                        ;
                    }
                }

            }
        }

        runClients(List.of(new Test1C1(), new Test1C2()));
        List<StockBook> testBook = storeManager.getBooksByISBN(new HashSet<>(List.of(TEST_ISBN)));
        assertEquals(1, testBook.size());
        assertEquals(0, testBook.get(0).getNumCopies());
    }



    /**
     * Test that the before-or-after atomicity
     *
     * @throws BookStoreException
     *             the book store exception
     */
    @Test
    public void testConcurrentDeadLock() throws InterruptedException, BookStoreException {
        int numCalls = 100;

        class RandomTransactionClient implements Runnable {
            public void run() {
                for (int i = 0; i < numCalls; i++) {
                    int randomNum = ThreadLocalRandom.current().nextInt(0, 9 + 1);
                    int randomISBN = TEST_ISBN + ThreadLocalRandom.current().nextInt(0, 4 + 1);
                    Set<Integer> isbnList = new HashSet<Integer>();
                    try{
                      switch (randomNum) {
                          case 0:
                              addBooks( randomISBN, 5);
                              break;
                          case 1:
                              Set<BookCopy> bookCopiesSet = new HashSet<BookCopy>();
                              bookCopiesSet.add(new BookCopy(randomISBN, 5));
                              storeManager.addCopies(bookCopiesSet);
                              break;
                          case 2:
                              isbnList.add(randomISBN);
                              storeManager.getBooks();
                              break;
                          case 3:
                              Set<BookEditorPick> editorPicksVals = new HashSet<BookEditorPick>();
                              editorPicksVals.add(new BookEditorPick(randomISBN, true));
                              storeManager.updateEditorPicks(editorPicksVals);
                              break;
                          case 4:
                              Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
                              booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES));
                              client.buyBooks(booksToBuy);
                              break;
                          case 5:
                              Set<Integer> isbnSet = new HashSet<Integer>();
                              isbnSet.add(randomISBN);
                              storeManager.getBooksByISBN(isbnSet);
                              break;
                          case 6:
                              isbnList.add(randomISBN);
                              client.getBooks(isbnList);
                              break;
                          case 7:
                              client.getEditorPicks(1);
                              break;
                          case 8:
                              storeManager.removeAllBooks();
                              break;
                          case 9:
                              storeManager.removeBooks(isbnList);
                              break;
                          default:
                              break;
                      }
                    } catch (BookStoreException ex) {
                        continue;
                    }
                }
            }
        }

        runClients(List.of(
                new RandomTransactionClient(),
                new RandomTransactionClient(),
                new RandomTransactionClient(),
                new RandomTransactionClient()
        ));
        assertTrue(true);
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
