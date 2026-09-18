package com.library.model;

/**
 * Status of a book reservation hold.
 */
public enum ReservationStatus {
    PENDING("Waiting in Queue"),
    NOTIFIED("Book Available - Notification Sent"),
    FULFILLED("Fulfilled / Issued to Patron"),
    CANCELLED("Cancelled");

    private final String display;

    ReservationStatus(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }
}
