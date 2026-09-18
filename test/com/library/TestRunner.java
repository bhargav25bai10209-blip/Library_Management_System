package com.library;

import com.library.exception.*;
import com.library.model.*;
import com.library.observer.NotificationService;
import com.library.repository.*;
import com.library.service.*;
import com.library.strategy.FacultyFineStrategy;
import com.library.strategy.FineCalculationStrategy;
import com.library.strategy.StudentFineStrategy;
import com.library.util.AnsiTheme;
import com.library.util.PasswordHasher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Pure Java Automated Test Runner and Validation Suite.
 * Validates domain constraints, design patterns, calculations, and persistence.
 */
public class TestRunner {

    private static int testsRun = 0;
    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println(AnsiTheme.bold(AnsiTheme.brightCyan("\n=======================================================")));
        System.out.println(AnsiTheme.bold(AnsiTheme.brightCyan("   LIBRITRACK PRO - AUTOMATED TEST & VALIDATION SUITE  ")));
        System.out.println(AnsiTheme.bold(AnsiTheme.brightCyan("=======================================================\n")));

        FileDataStore store = FileDataStore.getInstance();
        BookRepository bookRepo = new BookRepository(store);
        UserRepository userRepo = new UserRepository(store);
        TransactionRepository txRepo = new TransactionRepository(store);
        ReservationRepository resRepo = new ReservationRepository(store);
        FineReceiptRepository receiptRepo = new FineReceiptRepository(store);
        NotificationService notifService = NotificationService.getInstance();

        AuthService authService = new AuthService(userRepo, notifService);
        BookService bookService = new BookService(bookRepo, notifService);
        FineService fineService = new FineService(receiptRepo, userRepo, notifService);
        CirculationService circService = new CirculationService(bookRepo, userRepo, txRepo, resRepo, fineService, notifService);
        AnalyticsService analyticsService = new AnalyticsService(bookRepo, userRepo, txRepo, receiptRepo);

        runTest("Password Hashing & Salt Verification", () -> testPasswordHashing());
        runTest("Authentication & RBAC", () -> testAuthAndRbac(authService));
        runTest("Student & Faculty Registration Constraints", () -> testRegistration(authService));
        runTest("Book Catalog CRUD & Search", () -> testBookCatalog(bookService));
        runTest("Strategy Pattern: Student Fine Calculation", () -> testStudentFineStrategy());
        runTest("Strategy Pattern: Faculty Fine Calculation (With Grace Period)", () -> testFacultyFineStrategy());
        runTest("Circulation: Issue & Max Borrow Limit Enforcement", () -> testBorrowLimits(circService, userRepo, bookService));
        runTest("Circulation: Return & Overdue Penalty Assessment", () -> testReturnAndFines(circService, fineService));
        runTest("Priority Queue: Reservation Order (Faculty > Student)", () -> testReservationPriority(circService, userRepo, bookService));
        runTest("Fine Settlement & Receipt Generation", () -> testFinePayment(fineService, userRepo));
        runTest("System Analytics & Health Metrics", () -> testAnalytics(analyticsService));

        System.out.println("\n" + AnsiTheme.bold("-------------------------------------------------------"));
        System.out.println(String.format("Total Tests Run: %d | Passed: %s | Failed: %s",
                testsRun,
                AnsiTheme.brightGreen(String.valueOf(testsPassed)),
                (testsFailed > 0 ? AnsiTheme.brightRed(String.valueOf(testsFailed)) : "0")
        ));
        System.out.println(AnsiTheme.bold("-------------------------------------------------------\n"));

        if (testsFailed > 0) {
            System.exit(1);
        }
    }

    private static void runTest(String testName, TestCase testCase) {
        testsRun++;
        try {
            testCase.execute();
            testsPassed++;
            System.out.println(AnsiTheme.brightGreen("  [PASS] ") + testName);
        } catch (Throwable t) {
            testsFailed++;
            System.out.println(AnsiTheme.brightRed("  [FAIL] ") + testName + " -> " + t.getMessage());
            t.printStackTrace();
        }
    }

    private static void assertTrue(boolean condition, String msg) {
        if (!condition) throw new AssertionError("Assertion Failed: " + msg);
    }

    private static void assertEquals(Object expected, Object actual, String msg) {
        if (!expected.equals(actual)) {
            throw new AssertionError(String.format("%s [Expected: %s, Actual: %s]", msg, expected, actual));
        }
    }

    // --- TEST IMPLEMENTATIONS ---

    private static void testPasswordHashing() {
        String salt = PasswordHasher.generateSalt();
        String pass = "SecurePass123!";
        String hash = PasswordHasher.hashPassword(pass, salt);

        assertTrue(PasswordHasher.verifyPassword(pass, salt, hash), "Password should match correctly");
        assertTrue(!PasswordHasher.verifyPassword("WrongPass", salt, hash), "Wrong password must be rejected");
    }

    private static void testAuthAndRbac(AuthService authService) throws Exception {
        User admin = authService.login("admin@vityarthi.edu", "admin123");
        assertEquals(Role.ADMIN, admin.getRole(), "Admin role must be correctly resolved");

        User student = authService.login("aarav.sharma@vityarthi.edu", "student123");
        assertEquals(Role.STUDENT, student.getRole(), "Student role must be correctly resolved");

        try {
            authService.login("admin@vityarthi.edu", "wrongpass");
            throw new AssertionError("Invalid password must trigger AuthenticationException");
        } catch (AuthenticationException expected) {
            // Success
        }
    }

    private static void testRegistration(AuthService authService) throws Exception {
        String email = "test.newstudent" + System.currentTimeMillis() + "@vityarthi.edu";
        StudentMember sm = authService.registerStudent("Test Student", email, "9123456780", "mypassword", "Computer Science", 1);
        assertEquals(3, sm.getMaxBorrowLimit(), "Student max borrow limit must be 3");
        assertEquals(14, sm.getDefaultLoanPeriodDays(), "Student default loan period must be 14 days");
    }

    private static void testBookCatalog(BookService bookService) throws Exception {
        String isbn = "978-0131103627";
        Book book = bookService.getBook(isbn);
        assertTrue(book != null, "The C Programming Language book must exist");

        List<Book> searchResults = bookService.search("Algorithms");
        assertTrue(!searchResults.isEmpty(), "Search for 'Algorithms' must return matching books");
    }

    private static void testStudentFineStrategy() {
        FineCalculationStrategy strategy = new StudentFineStrategy();
        assertEquals(0.0, strategy.calculateFine(0), "No fine for 0 days overdue");
        assertEquals(5.0, strategy.calculateFine(1), "1 day overdue = ₹5.00");
        assertEquals(25.0, strategy.calculateFine(5), "5 days overdue = ₹25.00");
    }

    private static void testFacultyFineStrategy() {
        FineCalculationStrategy strategy = new FacultyFineStrategy();
        assertEquals(0.0, strategy.calculateFine(2), "Overdue <= 3 days has zero fine due to grace period");
        assertEquals(0.0, strategy.calculateFine(3), "Exactly 3 days overdue has zero fine");
        assertEquals(2.0, strategy.calculateFine(4), "4 days overdue (1 chargeable) = ₹2.00");
        assertEquals(6.0, strategy.calculateFine(6), "6 days overdue (3 chargeable) = ₹6.00");
    }

    private static void testBorrowLimits(CirculationService circService, UserRepository userRepo, BookService bookService) throws Exception {
        // Create an isolated test student
        String email = "borrow.test" + System.currentTimeMillis() + "@vityarthi.edu";
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hashPassword("pass123", salt);
        StudentMember testStu = new StudentMember("U-TST-001", "Limit Tester", email, "9999999991", hash, salt, "MEM-T-1", "CSE", 2);
        userRepo.save(testStu);

        // Add 4 test books
        Book b1 = getOrCreateBook(bookService, "978-1111111111", "Book One");
        Book b2 = getOrCreateBook(bookService, "978-2222222222", "Book Two");
        Book b3 = getOrCreateBook(bookService, "978-3333333333", "Book Three");
        Book b4 = getOrCreateBook(bookService, "978-4444444444", "Book Four");

        circService.issueBook(b1.getIsbn(), testStu.getUserId());
        circService.issueBook(b2.getIsbn(), testStu.getUserId());
        circService.issueBook(b3.getIsbn(), testStu.getUserId());

        // 4th borrow must fail because Student max limit is 3
        try {
            circService.issueBook(b4.getIsbn(), testStu.getUserId());
            throw new AssertionError("4th borrow should have thrown MaxBorrowLimitException");
        } catch (MaxBorrowLimitException expected) {
            // Success
        }

        // Clean up by returning 1 book
        circService.returnBook(b1.getIsbn(), testStu.getUserId(), LocalDate.now());
    }

    private static void testReturnAndFines(CirculationService circService, FineService fineService) throws Exception {
        // Find existing student Aarav
        String studentId = "U-STU-001";
        String isbn = "978-0134685991"; // Effective Java

        // Issue book
        try {
            circService.issueBook(isbn, studentId);
        } catch (Exception ignored) {
            // If already issued from prior run, proceed
        }

        // Return with simulated 4 days overdue
        LocalDate lateDate = LocalDate.now().plusDays(18); // 14-day limit + 4 days late
        Transaction tx = circService.returnBook(isbn, studentId, lateDate);

        assertTrue(tx.getFineAmount() > 0, "Fine amount should be greater than 0 for late return");
        assertEquals(20.0, tx.getFineAmount(), "4 days late for student @ ₹5/day = ₹20.00");
    }

    private static void testReservationPriority(CirculationService circService, UserRepository userRepo, BookService bookService) throws Exception {
        // Create an out of stock book with 0 copies available
        Book rareBook = getOrCreateBook(bookService, "978-9999999999", "Rare Manuscript");
        rareBook.setAvailableCopies(0);
        bookService.updateBook(rareBook.getIsbn(), rareBook.getTitle(), rareBook.getAuthor(), rareBook.getCategory(), rareBook.getPublicationYear(), rareBook.getTotalCopies(), rareBook.getRackLocation());

        // Student reserves first
        String stuId = "U-STU-001";
        Reservation rStu = circService.reserveBook(rareBook.getIsbn(), stuId);

        // Faculty reserves second
        String facId = "U-FAC-001";
        Reservation rFac = circService.reserveBook(rareBook.getIsbn(), facId);

        // Faculty priority (1) should come before Student priority (2) in queue ordering
        assertTrue(rFac.compareTo(rStu) < 0, "Faculty reservation priority must be ordered before student");
    }

    private static void testFinePayment(FineService fineService, UserRepository userRepo) throws Exception {
        User u = userRepo.findById("U-STU-001").orElseThrow();
        Member m = (Member) u;
        m.setOutstandingFines(50.0);
        userRepo.save(m);

        FineReceipt receipt = fineService.payFine(m.getUserId(), 20.0, "UPI");
        assertEquals(20.0, receipt.getAmountPaid(), "Payment receipt amount must match ₹20.00");

        User updatedUser = userRepo.findById("U-STU-001").orElseThrow();
        assertEquals(30.0, ((Member) updatedUser).getOutstandingFines(), "Remaining fine must be ₹30.00");
    }

    private static void testAnalytics(AnalyticsService analyticsService) {
        var overview = analyticsService.getSystemOverview();
        assertTrue((int) overview.get("distinctTitles") > 0, "Catalog must contain distinct titles");
        assertTrue((long) overview.get("studentCount") > 0, "There must be student members registered");
    }

    private static Book getOrCreateBook(BookService bookService, String isbn, String title) {
        try {
            return bookService.getBook(isbn);
        } catch (Exception e) {
            try {
                return bookService.addBook(isbn, title, "Test Author", BookCategory.COMPUTER_SCIENCE, 2021, 5, "Rack-TEST");
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @FunctionalInterface
    interface TestCase {
        void execute() throws Exception;
    }
}
