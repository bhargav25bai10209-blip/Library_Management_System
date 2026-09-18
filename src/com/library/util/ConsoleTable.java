package com.library.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility for rendering beautiful, aligned tabular data in terminal screens.
 */
public class ConsoleTable {

    private final List<String> headers = new ArrayList<>();
    private final List<List<String>> rows = new ArrayList<>();

    public ConsoleTable(String... headers) {
        this.headers.addAll(Arrays.asList(headers));
    }

    public void addRow(String... rowValues) {
        rows.add(Arrays.asList(rowValues));
    }

    public void print() {
        if (headers.isEmpty()) return;

        int cols = headers.size();
        int[] colWidths = new int[cols];

        for (int i = 0; i < cols; i++) {
            colWidths[i] = stripAnsi(headers.get(i)).length();
        }

        for (List<String> row : rows) {
            for (int i = 0; i < cols && i < row.size(); i++) {
                int len = stripAnsi(row.get(i)).length();
                if (len > colWidths[i]) {
                    colWidths[i] = len;
                }
            }
        }

        // Add padding
        for (int i = 0; i < cols; i++) {
            colWidths[i] += 2; // 1 space each side
        }

        // Top Border
        printSeparator("\u250C", "\u252C", "\u2510", colWidths);

        // Headers
        StringBuilder headerRow = new StringBuilder("\u2502");
        for (int i = 0; i < cols; i++) {
            headerRow.append(center(headers.get(i), colWidths[i])).append("\u2502");
        }
        System.out.println(AnsiTheme.BOLD + AnsiTheme.BRIGHT_CYAN + headerRow + AnsiTheme.RESET);

        // Header Separator
        printSeparator("\u251C", "\u253C", "\u2524", colWidths);

        // Data Rows
        if (rows.isEmpty()) {
            StringBuilder emptyRow = new StringBuilder("\u2502");
            int totalInnerWidth = Arrays.stream(colWidths).sum() + cols - 1;
            emptyRow.append(center(AnsiTheme.dim("No records found"), totalInnerWidth)).append("\u2502");
            System.out.println(emptyRow);
        } else {
            for (List<String> row : rows) {
                StringBuilder r = new StringBuilder("\u2502");
                for (int i = 0; i < cols; i++) {
                    String val = (i < row.size()) ? row.get(i) : "";
                    r.append(padRight(val, colWidths[i])).append("\u2502");
                }
                System.out.println(r);
            }
        }

        // Bottom Border
        printSeparator("\u2514", "\u2534", "\u2518", colWidths);
    }

    private void printSeparator(String left, String mid, String right, int[] widths) {
        StringBuilder sb = new StringBuilder(left);
        for (int i = 0; i < widths.length; i++) {
            sb.append("\u2500".repeat(widths[i]));
            if (i < widths.length - 1) {
                sb.append(mid);
            }
        }
        sb.append(right);
        System.out.println(AnsiTheme.DIM + sb + AnsiTheme.RESET);
    }

    private String center(String text, int width) {
        int visibleLen = stripAnsi(text).length();
        int pad = width - visibleLen;
        int leftPad = pad / 2;
        int rightPad = pad - leftPad;
        return " ".repeat(Math.max(0, leftPad)) + text + " ".repeat(Math.max(0, rightPad));
    }

    private String padRight(String text, int width) {
        int visibleLen = stripAnsi(text).length();
        int pad = width - visibleLen;
        return " " + text + " ".repeat(Math.max(0, pad - 1));
    }

    private String stripAnsi(String input) {
        if (input == null) return "";
        return input.replaceAll("\u001B\\[[;\\d]*m", "");
    }
}
