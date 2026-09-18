package com.library.service;

import com.library.exception.UserNotFoundException;
import com.library.model.*;
import com.library.observer.LibraryEvent;
import com.library.observer.NotificationService;
import com.library.repository.FineReceiptRepository;
import com.library.repository.UserRepository;
import com.library.strategy.FacultyFineStrategy;
import com.library.strategy.FineCalculationStrategy;
import com.library.strategy.StudentFineStrategy;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service managing penalty calculations using Strategy Pattern and payment processing.
 */
public class FineService {

    private final FineReceiptRepository receiptRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private final FineCalculationStrategy studentStrategy = new StudentFineStrategy();
    private final FineCalculationStrategy facultyStrategy = new FacultyFineStrategy();

    public FineService(FineReceiptRepository receiptRepository,
                       UserRepository userRepository,
                       NotificationService notificationService) {
        this.receiptRepository = receiptRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /**
     * Resolves the appropriate strategy based on runtime polymorphism.
     */
    public FineCalculationStrategy getStrategy(Member member) {
        if (member instanceof FacultyMember) {
            return facultyStrategy;
        }
        return studentStrategy;
    }

    /**
     * Calculates the overdue fine for a transaction at a specified date.
     */
    public double calculateFine(Transaction tx, Member member, LocalDate referenceDate) {
        long overdueDays = tx.calculateOverdueDays(referenceDate);
        if (overdueDays <= 0) {
            return 0.0;
        }
        FineCalculationStrategy strategy = getStrategy(member);
        return strategy.calculateFine(overdueDays);
    }

    /**
     * Records payment of overdue fines and produces a verifiable FineReceipt.
     */
    public synchronized FineReceipt payFine(String memberId, double amount, String paymentMethod)
            throws UserNotFoundException, IllegalArgumentException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }

        User u = userRepository.findById(memberId)
                .orElseThrow(() -> new UserNotFoundException("Member not found: " + memberId));

        if (!(u instanceof Member member)) {
            throw new IllegalArgumentException("Target user is not a library patron.");
        }

        if (member.getOutstandingFines() <= 0) {
            throw new IllegalArgumentException("Member has zero outstanding fines.");
        }

        double payable = Math.min(amount, member.getOutstandingFines());
        member.payFine(payable);
        userRepository.save(member);

        String receiptId = "RCP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        FineReceipt receipt = new FineReceipt(
                receiptId,
                "PAY-DIRECT",
                member.getUserId(),
                member.getName(),
                payable,
                paymentMethod != null ? paymentMethod : "CASH"
        );
        receiptRepository.save(receipt);

        notificationService.publish(new LibraryEvent(
                LibraryEvent.EventType.FINE_PAID,
                String.format("Payment of ₹%.2f processed. Receipt: %s. Remaining fine: ₹%.2f",
                        payable, receiptId, member.getOutstandingFines()),
                member.getUserId(),
                receipt
        ));

        return receipt;
    }

    public List<FineReceipt> getReceiptsByMember(String memberId) {
        return receiptRepository.findByMemberId(memberId);
    }

    public double getTotalRevenue() {
        return receiptRepository.getTotalRevenueCollected();
    }
}
