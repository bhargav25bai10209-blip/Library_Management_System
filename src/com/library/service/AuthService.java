package com.library.service;

import com.library.exception.AuthenticationException;
import com.library.exception.DuplicateEntityException;
import com.library.exception.UserNotFoundException;
import com.library.model.*;
import com.library.observer.LibraryEvent;
import com.library.observer.NotificationService;
import com.library.repository.UserRepository;
import com.library.util.InputValidator;
import com.library.util.PasswordHasher;

import java.util.Optional;
import java.util.UUID;

/**
 * Service managing user authentication, sessions, and registration.
 */
public class AuthService {

    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private User currentUser;

    public AuthService(UserRepository userRepository, NotificationService notificationService) {
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public synchronized User login(String email, String rawPassword) throws AuthenticationException {
        if (!InputValidator.isNotEmpty(email) || !InputValidator.isNotEmpty(rawPassword)) {
            throw new AuthenticationException("Email and password cannot be blank.");
        }

        Optional<User> userOpt = userRepository.findByEmail(email.trim());
        if (userOpt.isEmpty()) {
            throw new AuthenticationException("Invalid email or password.");
        }

        User user = userOpt.get();
        if (!user.isActive()) {
            throw new AuthenticationException("Account is deactivated. Contact Administrator.");
        }

        boolean valid = PasswordHasher.verifyPassword(rawPassword, user.getSalt(), user.getPasswordHash());
        if (!valid) {
            throw new AuthenticationException("Invalid email or password.");
        }

        this.currentUser = user;
        return user;
    }

    public synchronized void logout() {
        this.currentUser = null;
    }

    public synchronized User getCurrentUser() {
        return currentUser;
    }

    public synchronized boolean isLoggedIn() {
        return currentUser != null;
    }

    public synchronized StudentMember registerStudent(String name, String email, String phone,
                                                      String password, String department,
                                                      int academicYear) throws DuplicateEntityException, IllegalArgumentException {
        validateRegistration(email, password, name, phone);

        String userId = "U-STU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String membershipId = "MEM-S-" + (1000 + userRepository.findAll().size() + 1);
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hashPassword(password, salt);

        StudentMember student = new StudentMember(userId, name, email, phone, hash, salt, membershipId, department, academicYear);
        userRepository.save(student);

        notificationService.publish(new LibraryEvent(
                LibraryEvent.EventType.ACCOUNT_CREATED,
                "Welcome to the library! Your membership ID is " + membershipId,
                userId,
                student
        ));

        return student;
    }

    public synchronized FacultyMember registerFaculty(String name, String email, String phone,
                                                      String password, String department,
                                                      String designation) throws DuplicateEntityException, IllegalArgumentException {
        validateRegistration(email, password, name, phone);

        String userId = "U-FAC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String membershipId = "MEM-F-" + (2000 + userRepository.findAll().size() + 1);
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hashPassword(password, salt);

        FacultyMember faculty = new FacultyMember(userId, name, email, phone, hash, salt, membershipId, department, designation);
        userRepository.save(faculty);

        notificationService.publish(new LibraryEvent(
                LibraryEvent.EventType.ACCOUNT_CREATED,
                "Welcome to the library! Your faculty membership ID is " + membershipId,
                userId,
                faculty
        ));

        return faculty;
    }

    public synchronized Librarian registerLibrarian(String name, String email, String phone,
                                                    String password, String deskLocation)
            throws DuplicateEntityException, IllegalArgumentException {
        validateRegistration(email, password, name, phone);

        String userId = "U-LIB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String employeeId = "EMP-" + (3000 + userRepository.findAll().size() + 1);
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hashPassword(password, salt);

        Librarian librarian = new Librarian(userId, name, email, phone, hash, salt, employeeId, deskLocation);
        userRepository.save(librarian);
        return librarian;
    }

    public synchronized void changePassword(String userId, String currentPass, String newPass)
            throws UserNotFoundException, AuthenticationException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        if (!PasswordHasher.verifyPassword(currentPass, user.getSalt(), user.getPasswordHash())) {
            throw new AuthenticationException("Current password does not match.");
        }

        if (!InputValidator.isNotEmpty(newPass) || newPass.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters long.");
        }

        String newSalt = PasswordHasher.generateSalt();
        String newHash = PasswordHasher.hashPassword(newPass, newSalt);
        user.setSalt(newSalt);
        user.setPasswordHash(newHash);
        userRepository.save(user);
    }

    private void validateRegistration(String email, String password, String name, String phone) throws DuplicateEntityException {
        if (!InputValidator.isNotEmpty(name)) {
            throw new IllegalArgumentException("Name cannot be empty.");
        }
        if (!InputValidator.isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email address format.");
        }
        if (!InputValidator.isNotEmpty(password) || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new DuplicateEntityException("An account with email '" + email + "' already exists.");
        }
    }
}
