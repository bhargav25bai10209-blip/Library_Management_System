package com.library.repository;

import com.library.model.Transaction;
import com.library.model.TransactionStatus;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Repository managing book loans and returns history.
 */
public class TransactionRepository {

    private final Map<String, Transaction> transactionMap = new ConcurrentHashMap<>();
    private final FileDataStore fileDataStore;

    public TransactionRepository(FileDataStore fileDataStore) {
        this.fileDataStore = fileDataStore;
        reload();
    }

    public void reload() {
        transactionMap.clear();
        List<Transaction> txs = fileDataStore.loadTransactions();
        for (Transaction t : txs) {
            transactionMap.put(t.getTransactionId().toLowerCase().trim(), t);
        }
    }

    public synchronized void syncToFile() {
        fileDataStore.saveTransactions(new ArrayList<>(transactionMap.values()));
    }

    public Optional<Transaction> findById(String transactionId) {
        if (transactionId == null) return Optional.empty();
        return Optional.ofNullable(transactionMap.get(transactionId.toLowerCase().trim()));
    }

    public List<Transaction> findAll() {
        return new ArrayList<>(transactionMap.values());
    }

    public synchronized Transaction save(Transaction transaction) {
        transactionMap.put(transaction.getTransactionId().toLowerCase().trim(), transaction);
        syncToFile();
        return transaction;
    }

    public List<Transaction> findByMemberId(String memberId) {
        if (memberId == null) return Collections.emptyList();
        String mid = memberId.toLowerCase().trim();
        return transactionMap.values().stream()
                .filter(t -> t.getMemberId().equalsIgnoreCase(mid))
                .sorted(Comparator.comparing(Transaction::getIssueDate).reversed())
                .collect(Collectors.toList());
    }

    public List<Transaction> findActiveByMemberId(String memberId) {
        if (memberId == null) return Collections.emptyList();
        String mid = memberId.toLowerCase().trim();
        return transactionMap.values().stream()
                .filter(t -> t.getMemberId().equalsIgnoreCase(mid) && t.getStatus() == TransactionStatus.ISSUED)
                .collect(Collectors.toList());
    }

    public Optional<Transaction> findActiveLoan(String bookIsbn, String memberId) {
        if (bookIsbn == null || memberId == null) return Optional.empty();
        return transactionMap.values().stream()
                .filter(t -> t.getBookIsbn().equalsIgnoreCase(bookIsbn.trim())
                        && t.getMemberId().equalsIgnoreCase(memberId.trim())
                        && t.getStatus() == TransactionStatus.ISSUED)
                .findFirst();
    }

    public List<Transaction> findOverdueLoans(LocalDate today) {
        return transactionMap.values().stream()
                .filter(t -> t.getStatus() == TransactionStatus.ISSUED && t.getDueDate().isBefore(today))
                .sorted(Comparator.comparing(Transaction::getDueDate))
                .collect(Collectors.toList());
    }
}
