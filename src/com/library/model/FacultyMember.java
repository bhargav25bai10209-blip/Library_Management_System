package com.library.model;

/**
 * Represents a Faculty library patron.
 */
public class FacultyMember extends Member {
    private static final long serialVersionUID = 1L;

    private String department;
    private String designation;

    public FacultyMember() {
        super();
        this.role = Role.FACULTY;
    }

    public FacultyMember(String userId, String name, String email, String phone,
                         String passwordHash, String salt, String membershipId,
                         String department, String designation) {
        super(userId, name, email, phone, passwordHash, salt, Role.FACULTY, membershipId);
        this.department = department;
        this.designation = designation;
    }

    @Override
    public int getMaxBorrowLimit() {
        return 7; // Faculty members can borrow up to 7 books
    }

    @Override
    public int getDefaultLoanPeriodDays() {
        return 30; // 30-day borrowing duration
    }

    @Override
    public double getDailyFineRate() {
        return 2.0; // Concessional ₹2.00 per day
    }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
}
