package com.library.ui;

import com.library.model.*;
import com.library.observer.NotificationService;
import com.library.service.AuthService;
import com.library.service.BookService;
import com.library.service.CirculationService;
import com.library.service.FineService;
import com.library.util.AnsiTheme;
import com.library.util.ConsoleTable;

import java.util.List;

/**
 * Member console view for Student and Faculty library patrons.
 */
public class MemberView {

    private final AuthService authService;
    private final BookService bookService;
    private final CirculationService circulationService;
    private final FineService fineService;
    private final NotificationService notificationService;

    public MemberView(AuthService authService,
                      BookService bookService,
                      CirculationService circulationService,
                      FineService fineService,
                      NotificationService notificationService) {
        this.authService = authService;
        this.bookService = bookService;
        this.circulationService = circulationService;
        this.fineService = fineService;
        this.notificationService = notificationService;
    }

    public void display() {
        boolean back = false;
        while (!back) {
            User user = authService.getCurrentUser();
            if (!(user instanceof Member member)) {
                System.out.println(AnsiTheme.errorBadge("Active session is not a patron account."));
                return;
            }

            List<String> notifications = notificationService.getNotificationsForUser(member.getUserId());
            String notifBadge = notifications.isEmpty() ? "" : AnsiTheme.brightYellow(" [" + notifications.size() + " New Alert(s)!]");

            AnsiTheme.printBanner("LIBRITRACK PRO - MEMBER PORTAL", "Welcome, " + member.getName() + notifBadge);

            System.out.println(AnsiTheme.bold("1.") + " My Account & Membership Profile");
            System.out.println(AnsiTheme.bold("2.") + " Browse & Search Book Catalog");
            System.out.println(AnsiTheme.bold("3.") + " Reserve / Place Hold on Book");
            System.out.println(AnsiTheme.bold("4.") + " View My Active Loans");
            System.out.println(AnsiTheme.bold("5.") + " Renew Active Loan");
            System.out.println(AnsiTheme.bold("6.") + " View My Complete Borrowing History");
            System.out.println(AnsiTheme.bold("7.") + " View My Reservations / Waitlist Status");
            System.out.println(AnsiTheme.bold("8.") + " Notification Inbox" + notifBadge);
            System.out.println(AnsiTheme.bold("9.") + " View My Fines & Payment Receipts");
            System.out.println(AnsiTheme.bold("0.") + " Logout to Main Menu");
            System.out.println();

            int choice = ConsoleMenu.promptInt("Select an option", 0, 9);
            switch (choice) {
                case 1 -> showProfile(member);
                case 2 -> searchCatalog();
                case 3 -> reserveBook(member);
                case 4 -> viewMyActiveLoans(member);
                case 5 -> renewLoan();
                case 6 -> viewHistory(member);
                case 7 -> viewMyReservations(member);
                case 8 -> viewNotifications(member);
                case 9 -> viewFinesAndReceipts(member);
                case 0 -> {
                    authService.logout();
                    back = true;
                    System.out.println(AnsiTheme.infoBadge("Logged out successfully."));
                }
            }
        }
    }

    private void showProfile(Member member) {
        AnsiTheme.printBanner("PATRON PROFILE", member.getName());
        ConsoleTable table = new ConsoleTable("Attribute", "Details");
        table.addRow("User ID", member.getUserId());
        table.addRow("Full Name", member.getName());
        table.addRow("Email", member.getEmail());
        table.addRow("Role / Patron Tier", member.getRole().getTitle());
        table.addRow("Membership ID", member.getMembershipId());
        table.addRow("Active Issued Books", member.getActiveBorrowsCount() + " / " + member.getMaxBorrowLimit());
        table.addRow("Default Loan Duration", member.getDefaultLoanPeriodDays() + " Days");
        table.addRow("Daily Overdue Fine Rate", "₹" + String.format("%.2f", member.getDailyFineRate()) + " / day");
        String fineStr = (member.getOutstandingFines() > 0)
                ? AnsiTheme.brightRed("₹" + String.format("%.2f", member.getOutstandingFines()))
                : AnsiTheme.brightGreen("₹0.00 (Clear)");
        table.addRow("Outstanding Fines", fineStr);
        table.addRow("Account Status", member.isActive() ? AnsiTheme.brightGreen("ACTIVE") : AnsiTheme.brightRed("SUSPENDED"));

        if (member instanceof StudentMember sm) {
            table.addRow("Department", sm.getDepartment());
            table.addRow("Academic Year", "Year " + sm.getAcademicYear());
        } else if (member instanceof FacultyMember fm) {
            table.addRow("Department", fm.getDepartment());
            table.addRow("Designation", fm.getDesignation());
        }

        table.print();
        ConsoleMenu.pause();
    }

    private void searchCatalog() {
        AnsiTheme.printBanner("SEARCH LIBRARY CATALOG", "Find Books Available for Borrowing");
        String q = ConsoleMenu.prompt("Enter search keywords (or press Enter to view all)");
        List<Book> books = bookService.search(q);
        ConsoleMenu.displayBookTable(books);
        ConsoleMenu.pause();
    }

    private void reserveBook(Member member) {
        AnsiTheme.printBanner("RESERVE AN OUT-OF-STOCK BOOK", "Priority Hold Queue");
        String isbn = ConsoleMenu.prompt("Enter Book ISBN to reserve");
        try {
            Reservation res = circulationService.reserveBook(isbn, member.getUserId());
            System.out.println(AnsiTheme.successBadge("Book reserved successfully!"));
            System.out.println("  Reservation ID: " + res.getReservationId());
            System.out.println("  Book Title    : " + res.getBookTitle());
            System.out.println("  Priority Level: " + (res.getPriority() == 1 ? "Faculty (Priority 1)" : "Student (Standard 2)"));
            System.out.println("  You will receive a notification as soon as a copy is returned.");
        } catch (Exception e) {
            System.out.println(AnsiTheme.errorBadge("Reservation failed: " + e.getMessage()));
        }
        ConsoleMenu.pause();
    }

    private void viewMyActiveLoans(Member member) {
        AnsiTheme.printBanner("MY ACTIVE LOANS", "Currently Checked-Out Books");
        List<Transaction> active = circulationService.getActiveLoansByMember(member.getUserId());
        if (active.isEmpty()) {
            System.out.println(AnsiTheme.dim("You currently have no active book loans."));
        } else {
            ConsoleMenu.displayTransactionTable(active);
        }
        ConsoleMenu.pause();
    }

    private void renewLoan() {
        AnsiTheme.printBanner("RENEW BOOK LOAN", "Loan Duration Extension");
        String txId = ConsoleMenu.prompt("Enter Transaction ID of loan to renew");
        try {
            Transaction tx = circulationService.renewBook(txId);
            System.out.println(AnsiTheme.successBadge("Loan successfully renewed!"));
            System.out.println("  New Due Date : " + AnsiTheme.brightYellow(tx.getDueDate().toString()));
            System.out.println("  Renewals Used: " + tx.getRenewalCount() + "/2");
        } catch (Exception e) {
            System.out.println(AnsiTheme.errorBadge("Renewal failed: " + e.getMessage()));
        }
        ConsoleMenu.pause();
    }

    private void viewHistory(Member member) {
        AnsiTheme.printBanner("COMPLETE CIRCULATION HISTORY", "Past and Present Borrows");
        List<Transaction> history = circulationService.getLoanHistoryByMember(member.getUserId());
        if (history.isEmpty()) {
            System.out.println(AnsiTheme.dim("No past circulation transactions on record."));
        } else {
            ConsoleMenu.displayTransactionTable(history);
        }
        ConsoleMenu.pause();
    }

    private void viewMyReservations(Member member) {
        AnsiTheme.printBanner("MY RESERVATIONS", "Track Reserved Books");
        List<Reservation> list = circulationService.getReservationsByMember(member.getUserId());
        if (list.isEmpty()) {
            System.out.println(AnsiTheme.dim("You have no reservations."));
        } else {
            ConsoleMenu.displayReservationTable(list);
        }
        ConsoleMenu.pause();
    }

    private void viewNotifications(Member member) {
        AnsiTheme.printBanner("NOTIFICATIONS INBOX", "Real-Time System Alerts");
        List<String> notes = notificationService.getNotificationsForUser(member.getUserId());
        if (notes.isEmpty()) {
            System.out.println(AnsiTheme.dim("No notifications in your inbox."));
        } else {
            for (int i = 0; i < notes.size(); i++) {
                System.out.println(AnsiTheme.brightCyan((i + 1) + ". ") + notes.get(i));
            }
            System.out.println();
            String clear = ConsoleMenu.prompt("Clear all notifications? (y/n)");
            if ("y".equalsIgnoreCase(clear)) {
                notificationService.clearNotificationsForUser(member.getUserId());
                System.out.println(AnsiTheme.successBadge("Inbox cleared."));
            }
        }
        ConsoleMenu.pause();
    }

    private void viewFinesAndReceipts(Member member) {
        AnsiTheme.printBanner("FINES & PAYMENT RECEIPTS", "Financial Overview");
        System.out.println("Outstanding Balance: " + ((member.getOutstandingFines() > 0)
                ? AnsiTheme.brightRed("₹" + String.format("%.2f", member.getOutstandingFines()))
                : AnsiTheme.brightGreen("₹0.00")));
        System.out.println();

        List<FineReceipt> receipts = fineService.getReceiptsByMember(member.getUserId());
        if (receipts.isEmpty()) {
            System.out.println(AnsiTheme.dim("No payment receipts found."));
        } else {
            ConsoleTable table = new ConsoleTable("Receipt ID", "Amount Paid", "Method", "Payment Timestamp");
            for (FineReceipt r : receipts) {
                table.addRow(
                        r.getReceiptId(),
                        "₹" + String.format("%.2f", r.getAmountPaid()),
                        r.getPaymentMethod(),
                        r.getPaymentDate().format(ConsoleMenu.DATE_TIME_FMT)
                );
            }
            table.print();
        }
        ConsoleMenu.pause();
    }
}
