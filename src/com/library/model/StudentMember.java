package com.library.model;

/**
 * Represents a Student library patron.
 */
public class StudentMember extends Member {
    private static final long serialVersionUID = 1L;

    private String department;
    private int academicYear;

    public StudentMember() {
        super();
        this.role = Role.STUDENT;
    }

    public StudentMember(String userId, String name, String email, String phone,
                         String passwordHash, String salt, String membershipId,
                         String department, int academicYear) {
        super(userId, name, email, phone, passwordHash, salt, Role.STUDENT, membershipId);
        this.department = department;
        this.academicYear = academicYear;
    }

    @Override
    public int getMaxBorrowLimit() {
        return 3; // Maximum 3 books at a time for students
    }

    @Override
    public int getDefaultLoanPeriodDays() {
        return 14; // 14-day borrowing duration
    }

    @Override
    public double getDailyFineRate() {
        return 5.0; // ₹5.00 per day overdue
    }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public int getAcademicYear() { return academicYear; }
    public void setAcademicYear(int academicYear) { this.academicYear = academicYear; }
}
