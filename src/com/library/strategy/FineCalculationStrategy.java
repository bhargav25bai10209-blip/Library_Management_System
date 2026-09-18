package com.library.strategy;

/**
 * Strategy interface defining how overdue fines are computed for different patron tiers.
 */
public interface FineCalculationStrategy {
    /**
     * Computes fine amount based on overdue days.
     * @param overdueDays number of elapsed days past the due date
     * @return calculated fine in INR
     */
    double calculateFine(long overdueDays);

    /**
     * Number of allowable grace period days.
     */
    int getGracePeriodDays();

    /**
     * Daily penalty rate.
     */
    double getDailyRate();

    /**
     * Human-readable description of this strategy.
     */
    String getPolicyDescription();
}
