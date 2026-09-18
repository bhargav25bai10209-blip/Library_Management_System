package com.library.repository;

import com.library.model.FineReceipt;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Repository tracking payment receipts for fine collections.
 */
public class FineReceiptRepository {

    private final Map<String, FineReceipt> receiptMap = new ConcurrentHashMap<>();
    private final FileDataStore fileDataStore;

    public FineReceiptRepository(FileDataStore fileDataStore) {
        this.fileDataStore = fileDataStore;
        reload();
    }

    public void reload() {
        receiptMap.clear();
        List<FineReceipt> list = fileDataStore.loadReceipts();
        for (FineReceipt r : list) {
            receiptMap.put(r.getReceiptId().toLowerCase().trim(), r);
        }
    }

    public synchronized void syncToFile() {
        fileDataStore.saveReceipts(new ArrayList<>(receiptMap.values()));
    }

    public Optional<FineReceipt> findById(String receiptId) {
        if (receiptId == null) return Optional.empty();
        return Optional.ofNullable(receiptMap.get(receiptId.toLowerCase().trim()));
    }

    public List<FineReceipt> findAll() {
        return new ArrayList<>(receiptMap.values());
    }

    public synchronized FineReceipt save(FineReceipt receipt) {
        receiptMap.put(receipt.getReceiptId().toLowerCase().trim(), receipt);
        syncToFile();
        return receipt;
    }

    public List<FineReceipt> findByMemberId(String memberId) {
        if (memberId == null) return Collections.emptyList();
        String mid = memberId.toLowerCase().trim();
        return receiptMap.values().stream()
                .filter(r -> r.getMemberId().equalsIgnoreCase(mid))
                .sorted(Comparator.comparing(FineReceipt::getPaymentDate).reversed())
                .collect(Collectors.toList());
    }

    public double getTotalRevenueCollected() {
        return receiptMap.values().stream().mapToDouble(FineReceipt::getAmountPaid).sum();
    }
}
