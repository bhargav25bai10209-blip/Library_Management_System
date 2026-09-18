package com.library.ui;

import com.library.model.*;
import com.library.util.AnsiTheme;
import com.library.util.ConsoleTable;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

/**
 * Shared CLI UI rendering components and safe user input prompts.
 */
public class ConsoleMenu {

    public static final Scanner SCANNER = new Scanner(System.in);
    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static String prompt(String message) {
        System.out.print(AnsiTheme.BRIGHT_CYAN + message + ": " + AnsiTheme.RESET);
        return SCANNER.nextLine().trim();
    }

    public static int promptInt(String message, int min, int max) {
        while (true) {
            System.out.print(AnsiTheme.BRIGHT_CYAN + message + " [" + min + "-" + max + "]: " + AnsiTheme.RESET);
            String input = SCANNER.nextLine().trim();
            try {
                int val = Integer.parseInt(input);
                if (val >= min && val <= max) {
                    return val;
                }
                System.out.println(AnsiTheme.errorBadge("Input must be between " + min + " and " + max));
            } catch (NumberFormatException e) {
                System.out.println(AnsiTheme.errorBadge("Invalid integer. Please try again."));
            }
        }
    }

    public static double promptDouble(String message, double min) {
        while (true) {
            System.out.print(AnsiTheme.BRIGHT_CYAN + message + ": " + AnsiTheme.RESET);
            String input = SCANNER.nextLine().trim();
            try {
                double val = Double.parseDouble(input);
                if (val >= min) {
                    return val;
                }
                System.out.println(AnsiTheme.errorBadge("Value must be at least " + min));
            } catch (NumberFormatException e) {
                System.out.println(AnsiTheme.errorBadge("Invalid decimal number. Please try again."));
            }
        }
    }

    public static void pause() {
        System.out.print(AnsiTheme.DIM + "\nPress Enter to continue..." + AnsiTheme.RESET);
        SCANNER.nextLine();
    }

    public static void displayBookTable(List<Book> books) {
        ConsoleTable table = new ConsoleTable("ISBN", "Title", "Author", "Genre", "Year", "Copies", "Rack");
        for (Book b : books) {
            String copies = (b.isAvailable() ? AnsiTheme.brightGreen(b.getAvailableCopies() + "/" + b.getTotalCopies())
                                             : AnsiTheme.brightRed("0/" + b.getTotalCopies() + " [OUT]"));
            table.addRow(
                    b.getIsbn(),
                    truncate(b.getTitle(), 32),
                    truncate(b.getAuthor(), 20),
                    b.getCategory().getDisplayName(),
                    String.valueOf(b.getPublicationYear()),
                    copies,
                    b.getRackLocation() != null ? b.getRackLocation() : "N/A"
            );
        }
        table.print();
        System.out.println(AnsiTheme.dim("Total books displayed: " + books.size()));
    }

    public static void displayTransactionTable(List<Transaction> transactions) {
        ConsoleTable table = new ConsoleTable("TX ID", "Book ISBN / Title", "Patron ID", "Issue Date", "Due Date", "Status", "Fine");
        for (Transaction t : transactions) {
            String statusStr;
            switch (t.getStatus()) {
                case ISSUED: statusStr = AnsiTheme.brightBlue("ISSUED"); break;
                case RETURNED: statusStr = AnsiTheme.brightGreen("RETURNED"); break;
                case RETURNED_OVERDUE: statusStr = AnsiTheme.brightYellow("RET-OVERDUE"); break;
                case OVERDUE: statusStr = AnsiTheme.brightRed("OVERDUE"); break;
                default: statusStr = t.getStatus().name();
            }

            String fineStr = (t.getFineAmount() > 0)
                    ? AnsiTheme.brightRed("₹" + String.format("%.2f", t.getFineAmount()))
                    : AnsiTheme.dim("₹0.00");

            table.addRow(
                    t.getTransactionId(),
                    truncate(t.getBookTitle(), 26),
                    t.getMemberId(),
                    t.getIssueDate().format(DATE_FMT),
                    t.getDueDate().format(DATE_FMT),
                    statusStr,
                    fineStr
            );
        }
        table.print();
    }

    public static void displayReservationTable(List<Reservation> reservations) {
        ConsoleTable table = new ConsoleTable("Hold ID", "Book Title", "Patron", "Priority Tier", "Request Time", "Status");
        for (Reservation r : reservations) {
            String tier = (r.getPriority() == 1) ? AnsiTheme.magenta("Faculty (High)") : AnsiTheme.cyan("Student (Std)");
            String statusBadge;
            switch (r.getStatus()) {
                case PENDING: statusBadge = AnsiTheme.yellow("PENDING"); break;
                case NOTIFIED: statusBadge = AnsiTheme.brightGreen("NOTIFIED / READY"); break;
                case FULFILLED: statusBadge = AnsiTheme.dim("FULFILLED"); break;
                case CANCELLED: statusBadge = AnsiTheme.red("CANCELLED"); break;
                default: statusBadge = r.getStatus().name();
            }
            table.addRow(
                    r.getReservationId(),
                    truncate(r.getBookTitle(), 26),
                    r.getMemberName(),
                    tier,
                    r.getRequestDate().format(DATE_TIME_FMT),
                    statusBadge
            );
        }
        table.print();
    }

    public static String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
    }
}
