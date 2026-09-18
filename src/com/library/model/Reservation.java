package com.library.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents a reservation queue entry for a book with high demand.
 */
public class Reservation implements Serializable, Comparable<Reservation> {
    private static final long serialVersionUID = 1L;

    private String reservationId;
    private String bookIsbn;
    private String bookTitle;
    private String memberId;
    private String memberName;
    private int priority; // 1 = Faculty (Higher), 2 = Student (Standard)
    private LocalDateTime requestDate;
    private ReservationStatus status;
    private LocalDateTime notificationDate;

    public Reservation() {
        this.requestDate = LocalDateTime.now();
        this.status = ReservationStatus.PENDING;
    }

    public Reservation(String reservationId, String bookIsbn, String bookTitle,
                       String memberId, String memberName, int priority) {
        this.reservationId = reservationId;
        this.bookIsbn = bookIsbn;
        this.bookTitle = bookTitle;
        this.memberId = memberId;
        this.memberName = memberName;
        this.priority = priority;
        this.requestDate = LocalDateTime.now();
        this.status = ReservationStatus.PENDING;
    }

    @Override
    public int compareTo(Reservation other) {
        // Lower number means higher priority (e.g., Faculty priority 1 < Student priority 2)
        int priorityComp = Integer.compare(this.priority, other.priority);
        if (priorityComp != 0) {
            return priorityComp;
        }
        // If equal priority, FIFO by request timestamp
        return this.requestDate.compareTo(other.requestDate);
    }

    // Getters and Setters
    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }

    public String getBookIsbn() { return bookIsbn; }
    public void setBookIsbn(String bookIsbn) { this.bookIsbn = bookIsbn; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public LocalDateTime getRequestDate() { return requestDate; }
    public void setRequestDate(LocalDateTime requestDate) { this.requestDate = requestDate; }

    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }

    public LocalDateTime getNotificationDate() { return notificationDate; }
    public void setNotificationDate(LocalDateTime notificationDate) { this.notificationDate = notificationDate; }

    @Override
    public String toString() {
        return String.format("Res[%s] Book: '%s' (%s) -> Member: %s (Priority %d, Status: %s)",
                reservationId, bookTitle, bookIsbn, memberName, priority, status);
    }
}
