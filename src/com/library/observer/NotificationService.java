package com.library.observer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central event publisher and notification delivery service (Observer Pattern).
 */
public class NotificationService {
    private static NotificationService instance;

    private final List<LibraryEventListener> listeners = new CopyOnWriteArrayList<>();
    // User-specific notification inbox: userId -> list of notification strings
    private final Map<String, List<String>> userInboxes = new HashMap<>();

    private NotificationService() {
        // Register self as an internal listener to collect user notifications
        subscribe(this::handleInternalEvent);
    }

    public static synchronized NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    public void subscribe(LibraryEventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void unsubscribe(LibraryEventListener listener) {
        listeners.remove(listener);
    }

    public void publish(LibraryEvent event) {
        for (LibraryEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                System.err.println("Error dispatching event to listener: " + e.getMessage());
            }
        }
    }

    private void handleInternalEvent(LibraryEvent event) {
        if (event.getTargetUserId() != null) {
            String timestamp = event.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            String note = String.format("[%s] %s", timestamp, event.getMessage());
            synchronized (userInboxes) {
                userInboxes.computeIfAbsent(event.getTargetUserId(), k -> new ArrayList<>()).add(note);
            }
        }
    }

    public List<String> getNotificationsForUser(String userId) {
        synchronized (userInboxes) {
            List<String> notes = userInboxes.get(userId);
            if (notes == null || notes.isEmpty()) {
                return Collections.emptyList();
            }
            return new ArrayList<>(notes);
        }
    }

    public void clearNotificationsForUser(String userId) {
        synchronized (userInboxes) {
            userInboxes.remove(userId);
        }
    }
}
