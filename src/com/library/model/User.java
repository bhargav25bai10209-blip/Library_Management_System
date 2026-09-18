package com.library.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Abstract base class for all system users.
 */
public abstract class User implements Serializable {
    private static final long serialVersionUID = 1L;

    protected String userId;
    protected String name;
    protected String email;
    protected String phone;
    protected String passwordHash;
    protected String salt;
    protected Role role;
    protected boolean active;
    protected LocalDateTime registeredAt;

    public User() {
        this.active = true;
        this.registeredAt = LocalDateTime.now();
    }

    public User(String userId, String name, String email, String phone,
                String passwordHash, String salt, Role role) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.role = role;
        this.active = true;
        this.registeredAt = LocalDateTime.now();
    }

    // Abstract method to indicate permissions/borrow limit
    public abstract int getMaxBorrowLimit();

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
}
