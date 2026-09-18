package com.library.model;

/**
 * Status of a circulation loan transaction.
 */
public enum TransactionStatus {
    ISSUED("Currently Issued"),
    RETURNED("Returned On Time"),
    RETURNED_OVERDUE("Returned With Fine"),
    OVERDUE("Overdue / Action Required");

    private final String description;

    TransactionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
