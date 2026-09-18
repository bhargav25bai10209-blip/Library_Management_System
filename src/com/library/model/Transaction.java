package com.library.model;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Represents a book borrow/return circulation record.
 */
public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;

    private String transactionId;
    private String bookIsbn;
    private String bookTitle;
    private String memberId;
    private String memberName;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private double fineAmount;
    private boolean finePaid;
    private TransactionStatus status;
    private int renewalCount;

    public Transaction() {
        this.renewalCount = 0;
        this.fineAmount = 0.0;
        this.finePaid = false;
        this.status = TransactionStatus.ISSUED;
    }

    public Transaction(String transactionId, String bookIsbn, String bookTitle,
                       String memberId, String memberName, LocalDate issueDate, LocalDate dueDate) {
        this.transactionId = transactionId;
        this.bookIsbn = bookIsbn;
        this.bookTitle = bookTitle;
        this.memberId = memberId;
        this.memberName = memberName;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.returnDate = null;
        this.fineAmount = 0.0;
        this.finePaid = false;
        this.status = TransactionStatus.ISSUED;
        this.renewalCount = 0;
    }

    public boolean isOverdue(LocalDate referenceDate) {
        if (returnDate != null) {
            return returnDate.isAfter(dueDate);
        }
        return referenceDate.isAfter(dueDate);
    }

    public long calculateOverdueDays(LocalDate referenceDate) {
        LocalDate effectiveReturnDate = (returnDate != null) ? returnDate : referenceDate;
        if (effectiveReturnDate.isAfter(dueDate)) {
            return java.time.temporal.ChronoUnit.DAYS.between(dueDate, effectiveReturnDate);
        }
        return 0;
    }

    // Getters and Setters
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getBookIsbn() { return bookIsbn; }
    public void setBookIsbn(String bookIsbn) { this.bookIsbn = bookIsbn; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }

    public double getFineAmount() { return fineAmount; }
    public void setFineAmount(double fineAmount) { this.fineAmount = fineAmount; }

    public boolean isFinePaid() { return finePaid; }
    public void setFinePaid(boolean finePaid) { this.finePaid = finePaid; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public int getRenewalCount() { return renewalCount; }
    public void setRenewalCount(int renewalCount) { this.renewalCount = renewalCount; }

    @Override
    public String toString() {
        return String.format("Tx[%s] Book: %s (%s) -> Member: %s (%s) | Issued: %s | Due: %s | Status: %s | Fine: ₹%.2f",
                transactionId, bookTitle, bookIsbn, memberName, memberId, issueDate, dueDate, status, fineAmount);
    }
}
