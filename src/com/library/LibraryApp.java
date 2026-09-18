package com.library;

import com.library.model.Role;
import com.library.model.User;
import com.library.observer.NotificationService;
import com.library.repository.*;
import com.library.service.*;
import com.library.ui.AdminView;
import com.library.ui.ConsoleMenu;
import com.library.ui.LibrarianView;
import com.library.ui.MemberView;
import com.library.util.AnsiTheme;

/**
 * Main application entry point for LibriTrack Pro.
 * Bootstraps repositories, services, dependency injection, and presentation views.
 */
public class LibraryApp {

    private final AuthService authService;
    private final BookService bookService;
    private final MemberService memberService;
    private final CirculationService circulationService;
    private final FineService fineService;
    private final AnalyticsService analyticsService;

    private final AdminView adminView;
    private final LibrarianView librarianView;
    private final MemberView memberView;

    public LibraryApp() {
        // 1. Initialize Persistence and Repositories
        FileDataStore fileDataStore = FileDataStore.getInstance();
        BookRepository bookRepository = new BookRepository(fileDataStore);
        UserRepository userRepository = new UserRepository(fileDataStore);
        TransactionRepository transactionRepository = new TransactionRepository(fileDataStore);
        ReservationRepository reservationRepository = new ReservationRepository(fileDataStore);
        FineReceiptRepository receiptRepository = new FineReceiptRepository(fileDataStore);

        // 2. Initialize Event Broker
        NotificationService notificationService = NotificationService.getInstance();

        // 3. Initialize Domain Services
        this.authService = new AuthService(userRepository, notificationService);
        this.bookService = new BookService(bookRepository, notificationService);
        this.memberService = new MemberService(userRepository);
        this.fineService = new FineService(receiptRepository, userRepository, notificationService);
        this.circulationService = new CirculationService(
                bookRepository, userRepository, transactionRepository,
                reservationRepository, fineService, notificationService
        );
        this.analyticsService = new AnalyticsService(
                bookRepository, userRepository, transactionRepository, receiptRepository
        );

        // 4. Initialize Presentation Views
        this.adminView = new AdminView(authService, memberService, circulationService, analyticsService);
        this.librarianView = new LibrarianView(authService, bookService, circulationService, fineService);
        this.memberView = new MemberView(authService, bookService, circulationService, fineService, notificationService);

        // 5. Register JVM Graceful Shutdown Hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            bookRepository.syncToFile();
            userRepository.syncToFile();
            transactionRepository.syncToFile();
            reservationRepository.syncToFile();
            receiptRepository.syncToFile();
            System.out.println("\n" + AnsiTheme.dim("All library records synchronized safely to disk."));
        }));
    }

    public void start() {
        boolean exit = false;
        while (!exit) {
            AnsiTheme.printBanner(
                    "LIBRITRACK PRO - ADVANCED LIBRARY MANAGEMENT SYSTEM",
                    "Automated Circulation, Fine Calculation & Catalog Management"
            );

            System.out.println(AnsiTheme.bold("1.") + " Sign In (Existing User)");
            System.out.println(AnsiTheme.bold("2.") + " Register as Student Member");
            System.out.println(AnsiTheme.bold("3.") + " Register as Faculty Member");
            System.out.println(AnsiTheme.bold("4.") + " Public Catalog Search (Guest Mode)");
            System.out.println(AnsiTheme.bold("5.") + " Quick Demo Accounts Info");
            System.out.println(AnsiTheme.bold("0.") + " Exit System");
            System.out.println();

            int choice = ConsoleMenu.promptInt("Select an option", 0, 5);
            switch (choice) {
                case 1 -> handleLogin();
                case 2 -> handleStudentRegistration();
                case 3 -> handleFacultyRegistration();
                case 4 -> handlePublicCatalog();
                case 5 -> displayDemoAccounts();
                case 0 -> {
                    exit = true;
                    System.out.println(AnsiTheme.brightGreen("\nThank you for using LibriTrack Pro. Goodbye!\n"));
                }
            }
        }
    }

    private void handleLogin() {
        AnsiTheme.printBanner("SYSTEM LOGIN", "Authentication Portal");
        String email = ConsoleMenu.prompt("Email Address");
        String password = ConsoleMenu.prompt("Password");

        try {
            User user = authService.login(email, password);
            System.out.println(AnsiTheme.successBadge("Authentication successful! Welcome, " + user.getName()));
            routeByRole(user.getRole());
        } catch (Exception e) {
            System.out.println(AnsiTheme.errorBadge("Login failed: " + e.getMessage()));
            ConsoleMenu.pause();
        }
    }

    private void routeByRole(Role role) {
        switch (role) {
            case ADMIN -> adminView.display();
            case LIBRARIAN -> librarianView.display();
            case STUDENT, FACULTY -> memberView.display();
        }
    }

    private void handleStudentRegistration() {
        AnsiTheme.printBanner("STUDENT REGISTRATION", "Create New Student Patron Account");
        String name = ConsoleMenu.prompt("Full Name");
        String email = ConsoleMenu.prompt("Student University Email");
        String phone = ConsoleMenu.prompt("Phone Number");
        String dept = ConsoleMenu.prompt("Academic Department (e.g., Computer Science)");
        int year = ConsoleMenu.promptInt("Academic Year (1-4)", 1, 4);
        String pass = ConsoleMenu.prompt("Password (min 6 characters)");

        try {
            var student = authService.registerStudent(name, email, phone, pass, dept, year);
            System.out.println(AnsiTheme.successBadge("Student membership registered successfully!"));
            System.out.println("  Assigned User ID      : " + student.getUserId());
            System.out.println("  Assigned Membership ID: " + student.getMembershipId());
            System.out.println("  Borrowing Quota       : " + student.getMaxBorrowLimit() + " books simultaneously");
            System.out.println("  Loan Duration         : " + student.getDefaultLoanPeriodDays() + " days per book");
        } catch (Exception e) {
            System.out.println(AnsiTheme.errorBadge("Registration failed: " + e.getMessage()));
        }
        ConsoleMenu.pause();
    }

    private void handleFacultyRegistration() {
        AnsiTheme.printBanner("FACULTY REGISTRATION", "Create New Faculty Patron Account");
        String name = ConsoleMenu.prompt("Full Name (with title, e.g. Dr./Prof.)");
        String email = ConsoleMenu.prompt("Faculty University Email");
        String phone = ConsoleMenu.prompt("Phone Number");
        String dept = ConsoleMenu.prompt("Department / Faculty");
        String desig = ConsoleMenu.prompt("Designation (e.g., Assistant Professor)");
        String pass = ConsoleMenu.prompt("Password (min 6 characters)");

        try {
            var faculty = authService.registerFaculty(name, email, phone, pass, dept, desig);
            System.out.println(AnsiTheme.successBadge("Faculty membership registered successfully!"));
            System.out.println("  Assigned User ID      : " + faculty.getUserId());
            System.out.println("  Assigned Membership ID: " + faculty.getMembershipId());
            System.out.println("  Borrowing Quota       : " + faculty.getMaxBorrowLimit() + " books simultaneously");
            System.out.println("  Loan Duration         : " + faculty.getDefaultLoanPeriodDays() + " days per book (3-day grace period)");
        } catch (Exception e) {
            System.out.println(AnsiTheme.errorBadge("Registration failed: " + e.getMessage()));
        }
        ConsoleMenu.pause();
    }

    private void handlePublicCatalog() {
        AnsiTheme.printBanner("PUBLIC CATALOG SEARCH", "Guest Access");
        String q = ConsoleMenu.prompt("Enter search keywords (title, author, genre, or press Enter for all)");
        var list = bookService.search(q);
        ConsoleMenu.displayBookTable(list);
        ConsoleMenu.pause();
    }

    private void displayDemoAccounts() {
        AnsiTheme.printBanner("PRE-SEEDED DEMO ACCOUNTS", "Quick Evaluation Credentials");
        System.out.println(AnsiTheme.bold("1. Administrator:"));
        System.out.println("   Email   : admin@vityarthi.edu");
        System.out.println("   Password: admin123");
        System.out.println();
        System.out.println(AnsiTheme.bold("2. Librarian:"));
        System.out.println("   Email   : librarian@vityarthi.edu");
        System.out.println("   Password: lib123");
        System.out.println();
        System.out.println(AnsiTheme.bold("3. Student Member:"));
        System.out.println("   Email   : aarav.sharma@vityarthi.edu");
        System.out.println("   Password: student123");
        System.out.println();
        System.out.println(AnsiTheme.bold("4. Faculty Member:"));
        System.out.println("   Email   : vikram.reddy@vityarthi.edu");
        System.out.println("   Password: faculty123");
        System.out.println();
        ConsoleMenu.pause();
    }

    public static void main(String[] args) {
        LibraryApp app = new LibraryApp();
        app.start();
    }
}
