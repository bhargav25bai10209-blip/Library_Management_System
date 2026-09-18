package com.library.model;

/**
 * Base class representing library patrons (Members) who can borrow books.
 */
public abstract class Member extends User {
    private static final long serialVersionUID = 1L;

    protected String membershipId;
    protected double outstandingFines;
    protected int activeBorrowsCount;

    public Member() {
        super();
        this.outstandingFines = 0.0;
        this.activeBorrowsCount = 0;
    }

    public Member(String userId, String name, String email, String phone,
                  String passwordHash, String salt, Role role, String membershipId) {
        super(userId, name, email, phone, passwordHash, salt, role);
        this.membershipId = membershipId;
        this.outstandingFines = 0.0;
        this.activeBorrowsCount = 0;
    }

    public boolean canBorrow() {
        return active && (activeBorrowsCount < getMaxBorrowLimit()) && (outstandingFines <= 50.0);
    }

    public void incrementActiveBorrows() {
        this.activeBorrowsCount++;
    }

    public void decrementActiveBorrows() {
        if (this.activeBorrowsCount > 0) {
            this.activeBorrowsCount--;
        }
    }

    public void addFine(double amount) {
        if (amount > 0) {
            this.outstandingFines += amount;
        }
    }

    public void payFine(double amount) {
        if (amount > 0) {
            this.outstandingFines = Math.max(0.0, this.outstandingFines - amount);
        }
    }

    // Abstract contract for member loan duration and fine rate
    public abstract int getDefaultLoanPeriodDays();
    public abstract double getDailyFineRate();

    // Getters and Setters
    public String getMembershipId() { return membershipId; }
    public void setMembershipId(String membershipId) { this.membershipId = membershipId; }

    public double getOutstandingFines() { return outstandingFines; }
    public void setOutstandingFines(double outstandingFines) { this.outstandingFines = outstandingFines; }

    public int getActiveBorrowsCount() { return activeBorrowsCount; }
    public void setActiveBorrowsCount(int activeBorrowsCount) { this.activeBorrowsCount = activeBorrowsCount; }
}
