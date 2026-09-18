package com.library.model;

/**
 * Represents a System Administrator with full oversight privileges.
 */
public class Admin extends User {
    private static final long serialVersionUID = 1L;

    private String adminLevel;

    public Admin() {
        super();
        this.role = Role.ADMIN;
    }

    public Admin(String userId, String name, String email, String phone,
                 String passwordHash, String salt, String adminLevel) {
        super(userId, name, email, phone, passwordHash, salt, Role.ADMIN);
        this.adminLevel = adminLevel;
    }

    @Override
    public int getMaxBorrowLimit() {
        return 0; // Administrator role
    }

    public String getAdminLevel() { return adminLevel; }
    public void setAdminLevel(String adminLevel) { this.adminLevel = adminLevel; }
}
