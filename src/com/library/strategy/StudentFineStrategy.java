package com.library.strategy;

/**
 * Concrete fine calculation strategy for Student patrons.
 * Standard rate: ₹5.00 per day with zero grace period.
 */
public class StudentFineStrategy implements FineCalculationStrategy {

    private static final double DAILY_RATE = 5.0;
    private static final int GRACE_PERIOD = 0;

    @Override
    public double calculateFine(long overdueDays) {
        if (overdueDays <= 0) return 0.0;
        return overdueDays * DAILY_RATE;
    }

    @Override
    public int getGracePeriodDays() {
        return GRACE_PERIOD;
    }

    @Override
    public double getDailyRate() {
        return DAILY_RATE;
    }

    @Override
    public String getPolicyDescription() {
        return "Student Policy: ₹5.00/day overdue penalty (No grace period)";
    }
}
