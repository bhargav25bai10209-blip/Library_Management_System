package com.library.repository;

import com.library.model.Role;
import com.library.model.User;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Repository for user accounts and authentication profiles.
 */
public class UserRepository {

    private final Map<String, User> userMap = new ConcurrentHashMap<>();
    private final Map<String, String> emailToUserId = new ConcurrentHashMap<>();
    private final FileDataStore fileDataStore;

    public UserRepository(FileDataStore fileDataStore) {
        this.fileDataStore = fileDataStore;
        reload();
    }

    public void reload() {
        userMap.clear();
        emailToUserId.clear();
        List<User> users = fileDataStore.loadUsers();
        for (User u : users) {
            userMap.put(u.getUserId().toLowerCase().trim(), u);
            emailToUserId.put(u.getEmail().toLowerCase().trim(), u.getUserId().toLowerCase().trim());
        }
    }

    public synchronized void syncToFile() {
        fileDataStore.saveUsers(new ArrayList<>(userMap.values()));
    }

    public Optional<User> findById(String userId) {
        if (userId == null) return Optional.empty();
        return Optional.ofNullable(userMap.get(userId.toLowerCase().trim()));
    }

    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        String uid = emailToUserId.get(email.toLowerCase().trim());
        if (uid != null) {
            return findById(uid);
        }
        return Optional.empty();
    }

    public List<User> findAll() {
        return new ArrayList<>(userMap.values());
    }

    public List<User> findByRole(Role role) {
        return userMap.values().stream()
                .filter(u -> u.getRole() == role)
                .sorted(Comparator.comparing(User::getName))
                .collect(Collectors.toList());
    }

    public synchronized User save(User user) {
        userMap.put(user.getUserId().toLowerCase().trim(), user);
        emailToUserId.put(user.getEmail().toLowerCase().trim(), user.getUserId().toLowerCase().trim());
        syncToFile();
        return user;
    }

    public synchronized boolean delete(String userId) {
        if (userId == null) return false;
        User removed = userMap.remove(userId.toLowerCase().trim());
        if (removed != null) {
            emailToUserId.remove(removed.getEmail().toLowerCase().trim());
            syncToFile();
            return true;
        }
        return false;
    }
}
