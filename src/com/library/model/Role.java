package com.library.model;

/**
 * System roles for Role-Based Access Control (RBAC).
 */
public enum Role {
    ADMIN("Administrator"),
    LIBRARIAN("Librarian"),
    STUDENT("Student Member"),
    FACULTY("Faculty Member");

    private final String title;

    Role(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
