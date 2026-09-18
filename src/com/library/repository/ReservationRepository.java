package com.library.repository;

import com.library.model.Reservation;
import com.library.model.ReservationStatus;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Repository managing book reservations with priority-queue capabilities.
 */
public class ReservationRepository {

    private final Map<String, Reservation> reservationMap = new ConcurrentHashMap<>();
    private final FileDataStore fileDataStore;

    public ReservationRepository(FileDataStore fileDataStore) {
        this.fileDataStore = fileDataStore;
        reload();
    }

    public void reload() {
        reservationMap.clear();
        List<Reservation> list = fileDataStore.loadReservations();
        for (Reservation r : list) {
            reservationMap.put(r.getReservationId().toLowerCase().trim(), r);
        }
    }

    public synchronized void syncToFile() {
        fileDataStore.saveReservations(new ArrayList<>(reservationMap.values()));
    }

    public Optional<Reservation> findById(String reservationId) {
        if (reservationId == null) return Optional.empty();
        return Optional.ofNullable(reservationMap.get(reservationId.toLowerCase().trim()));
    }

    public List<Reservation> findAll() {
        return new ArrayList<>(reservationMap.values());
    }

    public synchronized Reservation save(Reservation reservation) {
        reservationMap.put(reservation.getReservationId().toLowerCase().trim(), reservation);
        syncToFile();
        return reservation;
    }

    public List<Reservation> findByMemberId(String memberId) {
        if (memberId == null) return Collections.emptyList();
        String mid = memberId.toLowerCase().trim();
        return reservationMap.values().stream()
                .filter(r -> r.getMemberId().equalsIgnoreCase(mid))
                .sorted(Comparator.comparing(Reservation::getRequestDate).reversed())
                .collect(Collectors.toList());
    }

    public Optional<Reservation> findPendingByBookAndMember(String isbn, String memberId) {
        if (isbn == null || memberId == null) return Optional.empty();
        return reservationMap.values().stream()
                .filter(r -> r.getBookIsbn().equalsIgnoreCase(isbn.trim())
                        && r.getMemberId().equalsIgnoreCase(memberId.trim())
                        && (r.getStatus() == ReservationStatus.PENDING || r.getStatus() == ReservationStatus.NOTIFIED))
                .findFirst();
    }

    /**
     * Retrieves the next patron in priority queue for a specific book.
     */
    public Optional<Reservation> peekNextInQueue(String isbn) {
        if (isbn == null) return Optional.empty();
        return reservationMap.values().stream()
                .filter(r -> r.getBookIsbn().equalsIgnoreCase(isbn.trim()) && r.getStatus() == ReservationStatus.PENDING)
                .min(Reservation::compareTo);
    }
}
