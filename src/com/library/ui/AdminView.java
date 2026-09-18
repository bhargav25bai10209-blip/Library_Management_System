package com.library.ui;

import com.library.model.*;
import com.library.service.AnalyticsService;
import com.library.service.AuthService;
import com.library.service.CirculationService;
import com.library.service.MemberService;
import com.library.util.AnsiTheme;
import com.library.util.ConsoleTable;

import java.util.List;
import java.util.Map;

/**
 * Administrative operations console view.
 */
public class AdminView {

    private final AuthService authService;
    private final MemberService memberService;
    private final CirculationService circulationService;
    private final AnalyticsService analyticsService;

    public AdminView(AuthService authService,
                     MemberService memberService,
                     CirculationService circulationService,
                     AnalyticsService analyticsService) {
        this.authService = authService;
        this.memberService = memberService;
        this.circulationService = circulationService;
        this.analyticsService = analyticsService;
    }

    public void display() {
        boolean back = false;
        while (!back) {
            User admin = authService.getCurrentUser();
            AnsiTheme.printBanner("LIBRITRACK PRO - ADMINISTRATOR CONSOLE", "Logged in as: " + admin.getName() + " (" + admin.getEmail() + ")");

            System.out.println(AnsiTheme.bold("1.") + " System Overview & Analytics Dashboard");
            System.out.println(AnsiTheme.bold("2.") + " View All Registered Users");
            System.out.println(AnsiTheme.bold("3.") + " Register New Librarian");
            System.out.println(AnsiTheme.bold("4.") + " Activate / Deactivate User Account");
            System.out.println(AnsiTheme.bold("5.") + " View Top Borrowed Books Analytics");
            System.out.println(AnsiTheme.bold("6.") + " View All Active Circulation Loans");
            System.out.println(AnsiTheme.bold("0.") + " Logout to Main Menu");
            System.out.println();

            int choice = ConsoleMenu.promptInt("Select an option", 0, 6);
            switch (choice) {
                case 1 -> showAnalytics();
                case 2 -> viewAllUsers();
                case 3 -> registerLibrarian();
                case 4 -> toggleUserStatus();
                case 5 -> showTopBooks();
                case 6 -> viewActiveLoans();
                case 0 -> {
                    authService.logout();
                    back = true;
                    System.out.println(AnsiTheme.infoBadge("Logged out successfully."));
                }
            }
        }
    }

    private void showAnalytics() {
        AnsiTheme.printBanner("SYSTEM ANALYTICS & INVENTORY HEALTH", "Real-time Metrics");
        Map<String, Object> stats = analyticsService.getSystemOverview();

        ConsoleTable table = new ConsoleTable("Metric Description", "Value");
        table.addRow("Distinct Book Titles in Catalog", String.valueOf(stats.get("distinctTitles")));
        table.addRow("Total Physical Copies in Stock", String.valueOf(stats.get("totalPhysicalCopies")));
        table.addRow("Available Copies On Shelf", AnsiTheme.brightGreen(String.valueOf(stats.get("availableCopies"))));
        table.addRow("Currently Borrowed Copies", AnsiTheme.brightYellow(String.valueOf(stats.get("issuedCopies"))));
        table.addRow("Registered Student Members", String.valueOf(stats.get("studentCount")));
        table.addRow("Registered Faculty Members", String.valueOf(stats.get("facultyCount")));
        table.addRow("Total Active Loans", String.valueOf(stats.get("activeLoansCount")));
        table.addRow("Currently Overdue Loans", AnsiTheme.brightRed(String.valueOf(stats.get("overdueLoansCount"))));
        table.addRow("Total Revenue Collected (Fines)", AnsiTheme.brightGreen("₹" + String.format("%.2f", (Double) stats.get("totalRevenueCollected"))));
        table.addRow("Total Unpaid / Outstanding Fines", AnsiTheme.brightRed("₹" + String.format("%.2f", (Double) stats.get("outstandingFinesTotal"))));
        table.print();

        ConsoleMenu.pause();
    }

    private void viewAllUsers() {
        AnsiTheme.printBanner("REGISTERED SYSTEM USERS", "Directory of Patrons and Staff");
        List<User> users = memberService.getAllUsers();

        ConsoleTable table = new ConsoleTable("User ID", "Name", "Role", "Email", "Phone", "Status", "Fines Due");
        for (User u : users) {
            String fines = "-";
            if (u instanceof Member m) {
                fines = (m.getOutstandingFines() > 0)
                        ? AnsiTheme.brightRed("₹" + String.format("%.2f", m.getOutstandingFines()))
                        : "₹0.00";
            }
            String status = u.isActive() ? AnsiTheme.brightGreen("ACTIVE") : AnsiTheme.brightRed("INACTIVE");

            table.addRow(
                    u.getUserId(),
                    u.getName(),
                    u.getRole().getTitle(),
                    u.getEmail(),
                    u.getPhone() != null ? u.getPhone() : "N/A",
                    status,
                    fines
            );
        }
        table.print();
        ConsoleMenu.pause();
    }

    private void registerLibrarian() {
        AnsiTheme.printBanner("REGISTER NEW LIBRARIAN", "Staff Credential Provisioning");
        String name = ConsoleMenu.prompt("Full Name");
        String email = ConsoleMenu.prompt("Email Address");
        String phone = ConsoleMenu.prompt("Phone Number");
        String desk = ConsoleMenu.prompt("Desk / Station Location (e.g. Desk B-2)");
        String password = ConsoleMenu.prompt("Initial Password (min 6 chars)");

        try {
            Librarian lib = authService.registerLibrarian(name, email, phone, password, desk);
            System.out.println(AnsiTheme.successBadge("Librarian registered successfully!"));
            System.out.println("  Assigned User ID   : " + lib.getUserId());
            System.out.println("  Assigned Employee ID: " + lib.getEmployeeId());
        } catch (Exception e) {
            System.out.println(AnsiTheme.errorBadge("Registration failed: " + e.getMessage()));
        }
        ConsoleMenu.pause();
    }

    private void toggleUserStatus() {
        AnsiTheme.printBanner("MANAGE USER STATUS", "Enable/Disable Account Access");
        String userId = ConsoleMenu.prompt("Enter Target User ID");
        try {
            memberService.toggleAccountStatus(userId);
            System.out.println(AnsiTheme.successBadge("Account status updated successfully for: " + userId));
        } catch (Exception e) {
            System.out.println(AnsiTheme.errorBadge("Operation failed: " + e.getMessage()));
        }
        ConsoleMenu.pause();
    }

    private void showTopBooks() {
        AnsiTheme.printBanner("TOP BORROWED BOOKS", "Popularity Analytics");
        List<Map.Entry<String, Long>> top = analyticsService.getTopBorrowedBooks(10);
        if (top.isEmpty()) {
            System.out.println(AnsiTheme.dim("No circulation records recorded yet."));
        } else {
            ConsoleTable table = new ConsoleTable("Rank", "Book Title", "Total Borrows Count");
            int rank = 1;
            for (Map.Entry<String, Long> entry : top) {
                table.addRow(String.valueOf(rank++), entry.getKey(), String.valueOf(entry.getValue()));
            }
            table.print();
        }
        ConsoleMenu.pause();
    }

    private void viewActiveLoans() {
        AnsiTheme.printBanner("ALL ACTIVE CIRCULATION LOANS", "Current Loans in Field");
        List<Transaction> active = circulationService.getAllActiveLoans();
        ConsoleMenu.displayTransactionTable(active);
        ConsoleMenu.pause();
    }
}
