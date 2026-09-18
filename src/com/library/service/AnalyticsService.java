package com.library.service;

import com.library.model.*;
import com.library.repository.BookRepository;
import com.library.repository.FineReceiptRepository;
import com.library.repository.TransactionRepository;
import com.library.repository.UserRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service providing statistical analytics, inventory health metrics, and audit summaries.
 */
public class AnalyticsService {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final FineReceiptRepository receiptRepository;

    public AnalyticsService(BookRepository bookRepository,
                            UserRepository userRepository,
                            TransactionRepository transactionRepository,
                            FineReceiptRepository receiptRepository) {
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.receiptRepository = receiptRepository;
    }

    public Map<String, Object> getSystemOverview() {
        Map<String, Object> stats = new LinkedHashMap<>();

        List<Book> books = bookRepository.findAll();
        int distinctTitles = books.size();
        int totalPhysicalCopies = books.stream().mapToInt(Book::getTotalCopies).sum();
        int availableCopies = books.stream().mapToInt(Book::getAvailableCopies).sum();
        int issuedCopies = totalPhysicalCopies - availableCopies;

        List<User> users = userRepository.findAll();
        long students = users.stream().filter(u -> u.getRole() == Role.STUDENT).count();
        long faculty = users.stream().filter(u -> u.getRole() == Role.FACULTY).count();
        long staff = users.stream().filter(u -> u.getRole() == Role.LIBRARIAN || u.getRole() == Role.ADMIN).count();

        List<Transaction> activeLoans = transactionRepository.findAll().stream()
                .filter(t -> t.getStatus() == TransactionStatus.ISSUED)
                .toList();

        List<Transaction> overdueLoans = transactionRepository.findOverdueLoans(LocalDate.now());

        double totalFinesCollected = receiptRepository.getTotalRevenueCollected();
        double outstandingFines = users.stream()
                .filter(u -> u instanceof Member)
                .mapToDouble(u -> ((Member) u).getOutstandingFines())
                .sum();

        stats.put("distinctTitles", distinctTitles);
        stats.put("totalPhysicalCopies", totalPhysicalCopies);
        stats.put("availableCopies", availableCopies);
        stats.put("issuedCopies", issuedCopies);
        stats.put("studentCount", students);
        stats.put("facultyCount", faculty);
        stats.put("staffCount", staff);
        stats.put("activeLoansCount", activeLoans.size());
        stats.put("overdueLoansCount", overdueLoans.size());
        stats.put("totalRevenueCollected", totalFinesCollected);
        stats.put("outstandingFinesTotal", outstandingFines);

        return stats;
    }

    public List<Map.Entry<String, Long>> getTopBorrowedBooks(int limit) {
        Map<String, Long> countMap = transactionRepository.findAll().stream()
                .collect(Collectors.groupingBy(Transaction::getBookTitle, Collectors.counting()));

        return countMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
}
