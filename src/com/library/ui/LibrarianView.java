package com.library.ui;

import com.library.model.*;
import com.library.service.AuthService;
import com.library.service.BookService;
import com.library.service.CirculationService;
import com.library.service.FineService;
import com.library.util.AnsiTheme;

import java.time.LocalDate;
import java.util.List;

/**
 * Librarian operational console view.
 */
public class LibrarianView {

    private final AuthService authService;
    private final BookService bookService;
    private final CirculationService circulationService;
    private final FineService fineService;

    public LibrarianView(AuthService authService,
                         BookService bookService,
                         CirculationService circulationService,
                         FineService fineService) {
        this.authService = authService;
        this.bookService = bookService;
        this.circulationService = circulationService;
        this.fineService = fineService;
    }

    public void display() {
        boolean back = false;
        while (!back) {
            User lib = authService.getCurrentUser();
            AnsiTheme.printBanner("LIBRITRACK PRO - LIBRARIAN CIRCULATION DESK", "Staff: " + lib.getName());

            System.out.println(AnsiTheme.bold("1.") + " Catalog: View All Books");
            System.out.println(AnsiTheme.bold("2.") + " Catalog: Search Catalog");
            System.out.println(AnsiTheme.bold("3.") + " Catalog: Add New Book");
            System.out.println(AnsiTheme.bold("4.") + " Catalog: Update Book Copies/Details");
            System.out.println(AnsiTheme.bold("5.") + " Catalog: Delete Book");
            System.out.println(AnsiTheme.bold("6.") + " Circulation: Issue Book to Patron");
            System.out.println(AnsiTheme.bold("7.") + " Circulation: Return Book (Process Returns & Fines)");
            System.out.println(AnsiTheme.bold("8.") + " Circulation: View Overdue Loans Report");
            System.out.println(AnsiTheme.bold("9.") + " Circulation: View All Active Reservations");
            System.out.println(AnsiTheme.bold("10.") + " Circulation: Collect Fine Payment & Issue Receipt");
            System.out.println(AnsiTheme.bold("0.") + " Logout to Main Menu");
            System.out.println();

            int choice = ConsoleMenu.promptInt("Select an option", 0, 10);
            switch (choice) {
                case 1 -> viewAllBooks();
                case 2 -> searchCatalog();
                case 3 -> addNewBook();
                case 4 -> updateBook();
                case 5 -> deleteBook();
                case 6 -> issueBook();
                case 7 -> returnBook();
                case 8 -> viewOverdueReport();
                case 9 -> viewReservations();
                case 10 -> collectFine();
                case 0 -> {
                    authService.logout();
                    back = true;
                    System.out.println(AnsiTheme.infoBadge("Logged out successfully."));
                }
            }
        }
    }

    private void viewAllBooks() {
        AnsiTheme.printBanner("CATALOG OVERVIEW", "Complete Book Collection");
        ConsoleMenu.displayBookTable(bookService.getAllBooks());
        ConsoleMenu.pause();
    }

    private void searchCatalog() {
        AnsiTheme.printBanner("SEARCH CATALOG", "Filter by Title, Author, ISBN, or Genre");
        String query = ConsoleMenu.prompt("Enter search keywords");
        List<Book> matches = bookService.search(query);
        ConsoleMenu.displayBookTable(matches);
        ConsoleMenu.pause();
    }

    private void addNewBook() {
        AnsiTheme.printBanner("CATALOG: ADD NEW BOOK", "Acquisition Intake");
        String isbn = ConsoleMenu.prompt("ISBN (10 or 13 digits)");
        String title = ConsoleMenu.prompt("Title");
        String author = ConsoleMenu.prompt("Author");

        System.out.println("\nSelect Category / Genre:");
        BookCategory[] categories = BookCategory.values();
        for (int i = 0; i < categories.length; i++) {
            System.out.println("  " + (i + 1) + ". " + categories[i].getDisplayName());
        }
        int catIndex = ConsoleMenu.promptInt("Category choice", 1, categories.length) - 1;
        BookCategory category = categories[catIndex];

        int year = ConsoleMenu.promptInt("Publication Year", 1500, 2030);
        int copies = ConsoleMenu.promptInt("Total Number of Copies", 1, 500);
        String rack = ConsoleMenu.prompt("Shelf / Rack Location (e.g. Rack-B-302)");

        try {
            Book added = bookService.addBook(isbn, title, author, category, year, copies, rack);
            System.out.println(AnsiTheme.successBadge("Book added to catalog successfully!"));
            System.out.println("  Details: " + added);
        } catch (Exception e) {
            System.out.println(AnsiTheme.errorBadge("Failed to add book: " + e.getMessage()));
        }
        ConsoleMenu.pause();
    }

    private void updateBook() {
        AnsiTheme.printBanner("CATALOG: UPDATE BOOK", "Modify Stock or Shelf Location");
        String isbn = ConsoleMenu.prompt("Enter ISBN of book to edit");
        try {
            Book existing = bookService.getBook(isbn);
            System.out.println(AnsiTheme.dim("Current record: " + existing));

            String title = ConsoleMenu.prompt("New Title (leave empty to keep: '" + existing.getTitle() + "')");
            if (title.isEmpty()) title = existing.getTitle();

            String author = ConsoleMenu.prompt("New Author (leave empty to keep: '" + existing.getAuthor() + "')");
            if (author.isEmpty()) author = existing.getAuthor();

            int copies = ConsoleMenu.promptInt("New Total Copies (currently " + existing.getTotalCopies() + ")", 0, 1000);
            String rack = ConsoleMenu.prompt("New Rack Location (leave empty to keep: '" + existing.getRackLocation() + "')");
            if (rack.isEmpty()) rack = existing.getRackLocation();

            Book updated = bookService.updateBook(isbn, title, author, existing.getCategory(), existing.getPublicationYear(), copies, rack);
            System.out.println(AnsiTheme.successBadge("Book updated successfully!"));
            System.out.println("  Updated record: " + updated);
        } catch (Exception e) {
            System.out.println(AnsiTheme.errorBadge("Update failed: " + e.getMessage()));
        }
        ConsoleMenu.pause();
    }

    private void deleteBook() {
        AnsiTheme.printBanner("CATALOG: DELETE BOOK", "Remove from Catalog");
        String isbn = ConsoleMenu.prompt("Enter ISBN to delete");
        try {
            bookService.deleteBook(isbn);
            System.out.println(AnsiTheme.successBadge("Book successfully deleted from catalog."));
        } catch (Exception e) {
            System.out.println(AnsiTheme.errorBadge("Delete failed: " + e.getMessage()));
        }
        ConsoleMenu.pause();
    }

    private void issueBook() {
        AnsiTheme.printBanner("CIRCULATION: ISSUE BOOK", "Loan Check-Out");
        String isbn = ConsoleMenu.prompt("Enter Book ISBN");
        String memberId = ConsoleMenu.prompt("Enter Patron User ID or Membership ID");

        try {
            Transaction tx = circulationService.issueBook(isbn, memberId);
            System.out.println(AnsiTheme.successBadge("Book successfully issued to patron!"));
            System.out.println("  Transaction ID : " + tx.getTransactionId());
            System.out.println("  Book           : " + tx.getBookTitle() + " (" + tx.getBookIsbn() + ")");
            System.out.println("  Patron         : " + tx.getMemberName() + " (" + tx.getMemberId() + ")");
            System.out.println("  Issue Date     : " + tx.getIssueDate());
            System.out.println("  Due Date       : " + AnsiTheme.brightYellow(tx.getDueDate().toString()));
        } catch (Exception e) {
            System.out.println(AnsiTheme.errorBadge("Issue operation failed: " + e.getMessage()));
        }
        ConsoleMenu.pause();
    }

    private void returnBook() {
        AnsiTheme.printBanner("CIRCULATION: RETURN BOOK", "Check-In Processing");
        String isbn = ConsoleMenu.prompt("Enter Book ISBN");
        String memberId = ConsoleMenu.prompt("Enter Patron User ID");

        System.out.println("Return date simulation option:");
        System.out.println("  1. Today (" + LocalDate.now() + ")");
        System.out.println("  2. Simulate past/future date (e.g. test overdue fines)");
        int dateOpt = ConsoleMenu.promptInt("Choice", 1, 2);

        LocalDate returnDate = LocalDate.now();
        if (dateOpt == 2) {
            String dateStr = ConsoleMenu.prompt("Enter return date (YYYY-MM-DD)");
            try {
                returnDate = LocalDate.parse(dateStr);
            } catch (Exception e) {
                System.out.println(AnsiTheme.warningBadge("Invalid format. Defaulting to today."));
            }
        }

        try {
            Transaction tx = circulationService.returnBook(isbn, memberId, returnDate);
            System.out.println(AnsiTheme.successBadge("Book return processed successfully!"));
            System.out.println("  Transaction ID : " + tx.getTransactionId());
            System.out.println("  Book           : " + tx.getBookTitle());
            System.out.println("  Return Date    : " + tx.getReturnDate());
            if (tx.getFineAmount() > 0) {
                System.out.println("  " + AnsiTheme.brightRed("OVERDUE PENALTY ASSESSED: ₹" + String.format("%.2f", tx.getFineAmount())));
            } else {
                System.out.println("  " + AnsiTheme.brightGreen("Returned on time! Zero fine incurred."));
            }
        } catch (Exception e) {
            System.out.println(AnsiTheme.errorBadge("Return processing failed: " + e.getMessage()));
        }
        ConsoleMenu.pause();
    }

    private void viewOverdueReport() {
        AnsiTheme.printBanner("OVERDUE LOANS AUDIT REPORT", "Action Required");
        List<Transaction> overdue = circulationService.getOverdueLoans();
        if (overdue.isEmpty()) {
            System.out.println(AnsiTheme.brightGreen("Great news! There are currently zero overdue loans."));
        } else {
            ConsoleMenu.displayTransactionTable(overdue);
        }
        ConsoleMenu.pause();
    }

    private void viewReservations() {
        AnsiTheme.printBanner("ALL ACTIVE RESERVATIONS", "Priority Waitlist");
        List<Reservation> list = circulationService.getAllReservations();
        ConsoleMenu.displayReservationTable(list);
        ConsoleMenu.pause();
    }

    private void collectFine() {
        AnsiTheme.printBanner("COLLECT FINE PAYMENT", "Settlement and Receipting");
        String memberId = ConsoleMenu.prompt("Enter Patron User ID");
        double amount = ConsoleMenu.promptDouble("Enter Payment Amount in INR (₹)", 1.0);
        String method = ConsoleMenu.prompt("Payment Method (CASH, CARD, UPI, NET_BANKING)");

        try {
            FineReceipt receipt = fineService.payFine(memberId, amount, method);
            System.out.println(AnsiTheme.successBadge("Fine payment settled successfully!"));
            System.out.println("  Receipt ID : " + receipt.getReceiptId());
            System.out.println("  Patron     : " + receipt.getMemberName() + " (" + receipt.getMemberId() + ")");
            System.out.println("  Amount Paid: ₹" + String.format("%.2f", receipt.getAmountPaid()));
            System.out.println("  Method     : " + receipt.getPaymentMethod());
            System.out.println("  Timestamp  : " + receipt.getPaymentDate());
        } catch (Exception e) {
            System.out.println(AnsiTheme.errorBadge("Fine collection failed: " + e.getMessage()));
        }
        ConsoleMenu.pause();
    }
}
