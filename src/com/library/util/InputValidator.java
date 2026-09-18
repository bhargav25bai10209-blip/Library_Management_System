package com.library.util;

import java.util.regex.Pattern;

/**
 * Utility for comprehensive input validation and sanitation.
 */
public final class InputValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    // Matches standard ISBN-10 or ISBN-13 (allowing optional hyphens)
    private static final Pattern ISBN_PATTERN = Pattern.compile(
            "^(?:ISBN(?:-1[03])?:?\\s*)?(?=[-0-9X]{10,17}$)(?:97[89][- ]?)?[0-9]{1,5}[- ]?[0-9]+[- ]?[0-9]+[- ]?[0-9X]$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?[0-9]{10,13}$"
    );

    private InputValidator() {}

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidIsbn(String isbn) {
        if (isbn == null) return false;
        String clean = isbn.trim().replaceAll("[\\s-]", "");
        // Check length 10 or 13 digits (with possible X at the end of 10)
        return (clean.length() == 10 || clean.length() == 13);
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone.trim().replaceAll("[\\s-]", "")).matches();
    }

    public static boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }

    public static boolean isPositiveInt(int value) {
        return value > 0;
    }

    public static boolean isNonNegativeInt(int value) {
        return value >= 0;
    }

    public static String sanitize(String input) {
        if (input == null) return "";
        return input.trim().replaceAll("[\r\n\t]", " ");
    }
}
