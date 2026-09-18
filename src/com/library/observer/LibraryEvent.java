package com.library.observer;

import java.time.LocalDateTime;

/**
 * Event object dispatched across library subsystems.
 */
public class LibraryEvent {
    public enum EventType {
        BOOK_ADDED,
        BOOK_ISSUED,
        BOOK_RETURNED,
        BOOK_RESERVED,
        RESERVATION_AVAILABLE,
        FINE_PAID,
        ACCOUNT_CREATED
    }

    private final EventType type;
    private final String message;
    private final String targetUserId;
    private final Object payload;
    private final LocalDateTime timestamp;

    public LibraryEvent(EventType type, String message, String targetUserId, Object payload) {
        this.type = type;
        this.message = message;
        this.targetUserId = targetUserId;
        this.payload = payload;
        this.timestamp = LocalDateTime.now();
    }

    public EventType getType() { return type; }
    public String getMessage() { return message; }
    public String getTargetUserId() { return targetUserId; }
    public Object getPayload() { return payload; }
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("[%s] %s (Target: %s)", type, message, targetUserId != null ? targetUserId : "ALL");
    }
}
