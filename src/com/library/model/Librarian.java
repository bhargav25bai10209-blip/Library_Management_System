package com.library.model;

/**
 * Represents a Librarian staff member with circulation privileges.
 */
public class Librarian extends User {
    private static final long serialVersionUID = 1L;

    private String employeeId;
    private String deskLocation;

    public Librarian() {
        super();
        this.role = Role.LIBRARIAN;
    }

    public Librarian(String userId, String name, String email, String phone,
                     String passwordHash, String salt, String employeeId, String deskLocation) {
        super(userId, name, email, phone, passwordHash, salt, Role.LIBRARIAN);
        this.employeeId = employeeId;
        this.deskLocation = deskLocation;
    }

    @Override
    public int getMaxBorrowLimit() {
        return 0; // Staff accounts are operational, not patrons
    }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getDeskLocation() { return deskLocation; }
    public void setDeskLocation(String deskLocation) { this.deskLocation = deskLocation; }
}
