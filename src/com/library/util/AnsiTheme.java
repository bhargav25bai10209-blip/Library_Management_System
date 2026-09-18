package com.library.util;

/**
 * ANSI Escape sequences and styling themes for modern, visually stunning terminal rendering.
 */
public final class AnsiTheme {

    // Style codes
    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String DIM = "\u001B[2m";
    public static final String ITALIC = "\u001B[3m";
    public static final String UNDERLINE = "\u001B[4m";

    // Foreground colors
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    // Bright colors
    public static final String BRIGHT_RED = "\u001B[91m";
    public static final String BRIGHT_GREEN = "\u001B[92m";
    public static final String BRIGHT_YELLOW = "\u001B[93m";
    public static final String BRIGHT_BLUE = "\u001B[94m";
    public static final String BRIGHT_MAGENTA = "\u001B[95m";
    public static final String BRIGHT_CYAN = "\u001B[96m";
    public static final String BRIGHT_WHITE = "\u001B[97m";

    // Background colors
    public static final String BG_BLUE = "\u001B[44m";
    public static final String BG_CYAN = "\u001B[46m";
    public static final String BG_DARK_GRAY = "\u001B[100m";

    private AnsiTheme() {}

    public static String cyan(String text) { return CYAN + text + RESET; }
    public static String brightCyan(String text) { return BRIGHT_CYAN + text + RESET; }
    public static String green(String text) { return GREEN + text + RESET; }
    public static String brightGreen(String text) { return BRIGHT_GREEN + text + RESET; }
    public static String yellow(String text) { return YELLOW + text + RESET; }
    public static String brightYellow(String text) { return BRIGHT_YELLOW + text + RESET; }
    public static String red(String text) { return RED + text + RESET; }
    public static String brightRed(String text) { return BRIGHT_RED + text + RESET; }
    public static String blue(String text) { return BLUE + text + RESET; }
    public static String brightBlue(String text) { return BRIGHT_BLUE + text + RESET; }
    public static String magenta(String text) { return MAGENTA + text + RESET; }
    public static String bold(String text) { return BOLD + text + RESET; }
    public static String dim(String text) { return DIM + text + RESET; }

    public static String successBadge(String text) {
        return BRIGHT_GREEN + "[\u2713 " + text + "]" + RESET;
    }

    public static String errorBadge(String text) {
        return BRIGHT_RED + "[\u2717 " + text + "]" + RESET;
    }

    public static String warningBadge(String text) {
        return BRIGHT_YELLOW + "[! " + text + "]" + RESET;
    }

    public static String infoBadge(String text) {
        return BRIGHT_CYAN + "[\u2139 " + text + "]" + RESET;
    }

    public static void printBanner(String title, String subtitle) {
        int width = 76;
        String line = "\u2550".repeat(width);
        System.out.println(BRIGHT_CYAN + line + RESET);
        System.out.println(BOLD + BRIGHT_WHITE + centerText(title, width) + RESET);
        if (subtitle != null && !subtitle.isEmpty()) {
            System.out.println(CYAN + centerText(subtitle, width) + RESET);
        }
        System.out.println(BRIGHT_CYAN + line + RESET);
    }

    private static String centerText(String text, int width) {
        int pad = (width - text.length()) / 2;
        if (pad <= 0) return text;
        return " ".repeat(pad) + text;
    }
}
