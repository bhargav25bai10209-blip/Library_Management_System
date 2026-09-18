package com.library.strategy;

/**
 * Concrete fine calculation strategy for Faculty patrons.
 * Concessional rate: ₹2.00 per day with 3-day grace period.
 */
public class FacultyFineStrategy implements FineCalculationStrategy {

    private static final double DAILY_RATE = 2.0;
    private static final int GRACE_PERIOD = 3;

    @Override
    public double calculateFine(long overdueDays) {
        if (overdueDays <= GRACE_PERIOD) {
            return 0.0;
        }
        long chargeableDays = overdueDays - GRACE_PERIOD;
        return chargeableDays * DAILY_RATE;
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
        return "Faculty Policy: ₹2.00/day overdue penalty (3-Day Grace Period)";
    }
}
