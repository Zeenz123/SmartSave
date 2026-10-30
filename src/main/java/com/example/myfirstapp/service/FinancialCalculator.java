package com.example.myfirstapp.service;

/**
 * Dedicated, centralized business-logic engine for all financial balance,
 * savings target, locked funds, Release Coin unlocking, and withdrawal calculations.
 *
 * Enforces the core mathematical rules:
 * 1. Balance = Total virtual money currently held (availableBalance + lockedSavings).
 * 2. Effective Balance = max(0, Balance - Locked Savings + Released Savings).
 *    Under full target commitment with Release Coin unlocking capacity:
 *    Effective Balance = min(Balance, Release Coins).
 *    Locked Savings = max(0, Balance - Release Coins).
 * 3. Release Coins represent unlocking capacity (1 Coin = ₹1 virtual value);
 *    they are NEVER subtracted from Balance as spent money.
 * 4. Target Completion: When Balance >= Target (and Target > 0), committed savings unlock completely.
 * 5. Invariant: Total Balance remains strictly constant during coin redemption. No new money created.
 */
public final class FinancialCalculator {

    private FinancialCalculator() {
        // Prevent instantiation of pure utility class
    }

    /**
     * Rounds amounts to 2 decimal places (standard currency cents/paise).
     */
    public static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * Calculates the total balance currently held in the user's wallet.
     * Balance = availableBalance + lockedSavings.
     */
    public static double calculateTotalBalance(double availableBalance, double lockedSavings) {
        return round(Math.max(0.0, availableBalance) + Math.max(0.0, lockedSavings));
    }

    /**
     * Determines whether the savings target has been reached.
     */
    public static boolean isTargetCompleted(double totalBalance, double targetBalance) {
        return targetBalance > 0.0 && totalBalance >= targetBalance;
    }

    /**
     * Calculates Effective Balance (withdrawable/spendable amount) using the core formula:
     * Effective Balance = max(0, min(Balance, Balance - Locked Savings + Released Savings))
     */
    public static double calculateEffectiveBalance(double totalBalance, double lockedSavings, double releasedSavings) {
        if (totalBalance <= 0.0) {
            return 0.0;
        }
        double effective = totalBalance - Math.max(0.0, lockedSavings) + Math.max(0.0, releasedSavings);
        return round(Math.max(0.0, Math.min(totalBalance, effective)));
    }

    /**
     * Core formula for a savings balance governed by a commitment target where
     * Release Coins act as unlocking capacity:
     *
     * If the savings target is completed, all funds are unlocked: Effective Balance = Balance.
     * Otherwise, if the funds are committed towards the target:
     * Effective Balance = min(Balance, Release Coins)
     *
     * Note: Release Coins are NEVER deducted from the balance.
     */
    public static double calculateEffectiveBalanceUnderCommitment(double totalBalance, double releaseCoins, double targetBalance) {
        if (totalBalance <= 0.0) {
            return 0.0;
        }
        if (isTargetCompleted(totalBalance, targetBalance)) {
            return round(totalBalance);
        }
        return round(Math.max(0.0, Math.min(totalBalance, Math.max(0.0, releaseCoins))));
    }

    /**
     * Calculates the locked savings under target commitment and Release Coin unlocking capacity:
     *
     * If target completed: Locked Savings = 0.
     * Otherwise:
     * Locked Savings = max(0, Balance - Release Coins)
     */
    public static double calculateLockedSavings(double totalBalance, double releaseCoins, double targetBalance) {
        if (totalBalance <= 0.0) {
            return 0.0;
        }
        if (isTargetCompleted(totalBalance, targetBalance)) {
            return 0.0;
        }
        return round(Math.max(0.0, totalBalance - Math.max(0.0, releaseCoins)));
    }

    /**
     * Determines how much locked savings can be unlocked when a user redeems Release Coins.
     *
     * Rules:
     * 1. 1 Release Coin = ₹1 of virtual value unlocking capacity.
     * 2. Cannot redeem more coins than currently available.
     * 3. Cannot unlock more than the currently locked savings.
     * 4. The actual unlockable amount = min(lockedSavings, min(availableCoins, coinsToRedeem)).
     *
     * @param lockedSavings Current locked savings in wallet (₹)
     * @param availableReleaseCoins Total unused Release Coins held by user
     * @param coinsToRedeem Requested number of coins to redeem
     * @return The exact amount (₹) of locked savings eligible to be unlocked into available balance
     */
    public static double calculateUnlockableAmount(double lockedSavings, double availableReleaseCoins, double coinsToRedeem) {
        if (lockedSavings <= 0.0 || availableReleaseCoins <= 0.0 || coinsToRedeem <= 0.0) {
            return 0.0;
        }
        double usableCoins = Math.min(availableReleaseCoins, coinsToRedeem);
        return round(Math.max(0.0, Math.min(lockedSavings, usableCoins)));
    }

    /**
     * Result of a target update or recalculation.
     */
    public static class TargetRecalculationResult {
        private final double totalBalance;
        private final double availableBalance;
        private final double lockedSavings;
        private final double targetBalance;
        private final boolean targetCompleted;

        public TargetRecalculationResult(double totalBalance, double availableBalance,
                                         double lockedSavings, double targetBalance, boolean targetCompleted) {
            this.totalBalance = round(totalBalance);
            this.availableBalance = round(availableBalance);
            this.lockedSavings = round(lockedSavings);
            this.targetBalance = round(targetBalance);
            this.targetCompleted = targetCompleted;
        }

        public double getTotalBalance() { return totalBalance; }
        public double getAvailableBalance() { return availableBalance; }
        public double getLockedSavings() { return lockedSavings; }
        public double getTargetBalance() { return targetBalance; }
        public boolean isTargetCompleted() { return targetCompleted; }
    }

    /**
     * Recalculates locked savings and available balance when a savings target is modified.
     *
     * Rules:
     * - If target is completed (totalBalance >= newTarget and newTarget > 0):
     *   Unlock committed savings: lockedSavings = 0, availableBalance = totalBalance.
     * - If target is increased and now exceeds totalBalance:
     *   The savings commitment is incomplete.
     *   Preserves total balance invariant (available + locked == totalBalance).
     *   Savings committed to the new target remain locked up to the committed portion.
     */
    public static TargetRecalculationResult recalculateOnTargetChange(
            double currentAvailable, double currentLocked, double currentTarget, double newTarget) {

        double total = calculateTotalBalance(currentAvailable, currentLocked);
        double validNewTarget = round(Math.max(0.0, newTarget));

        if (validNewTarget <= 0.0) {
            // Target cleared: all funds available
            return new TargetRecalculationResult(total, total, 0.0, 0.0, true);
        }

        if (total >= validNewTarget) {
            // Target achieved: unlock all committed savings
            return new TargetRecalculationResult(total, total, 0.0, validNewTarget, true);
        }

        // Target incomplete: total < validNewTarget
        // User committed to reaching newTarget.
        // If target was increased, we ensure locked portion reflects the commitment
        // up to total balance, without exceeding total balance.
        double newLocked;
        if (currentLocked > 0.0) {
            // If already locked, keep locked amount up to total balance
            newLocked = Math.min(total, currentLocked);
        } else {
            // If previously unlocked (e.g. target was 0 or completed earlier, now increased),
            // commit existing balance toward the new target
            newLocked = Math.min(total, validNewTarget);
        }

        double newAvailable = round(Math.max(0.0, total - newLocked));
        return new TargetRecalculationResult(total, newAvailable, round(newLocked), validNewTarget, false);
    }
}
