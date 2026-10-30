package com.example.myfirstapp.model;

import com.example.myfirstapp.service.FinancialCalculator;

import java.util.Locale;

/**
 * Model representing the user's virtual wallet.
 *
 * Core Financial Invariants:
 * - Total Balance = total virtual money currently held in wallet (availableBalance + lockedSavings).
 * - Effective Balance = withdrawable / spendable amount.
 * - Release Coins represent unlocking capacity (1 Coin = ₹1 virtual value) that cancels out money lockup.
 * - Target Balance governs when savings commitment is completed.
 */
public class Wallet {
    private int userId;
    private double availableBalance;
    private double lockedSavings;
    private double targetBalance;
    private double releaseCoins;

    public Wallet() {
    }

    public Wallet(int userId, double availableBalance, double lockedSavings, double targetBalance, double releaseCoins) {
        this.userId = userId;
        this.availableBalance = FinancialCalculator.round(Math.max(0.0, availableBalance));
        this.lockedSavings = FinancialCalculator.round(Math.max(0.0, lockedSavings));
        this.targetBalance = FinancialCalculator.round(Math.max(0.0, targetBalance));
        this.releaseCoins = FinancialCalculator.round(Math.max(0.0, releaseCoins));
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public double getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(double availableBalance) {
        this.availableBalance = FinancialCalculator.round(Math.max(0.0, availableBalance));
    }

    public double getLockedSavings() {
        return lockedSavings;
    }

    public void setLockedSavings(double lockedSavings) {
        this.lockedSavings = FinancialCalculator.round(Math.max(0.0, lockedSavings));
    }

    public double getTargetBalance() {
        return targetBalance;
    }

    public void setTargetBalance(double targetBalance) {
        this.targetBalance = FinancialCalculator.round(Math.max(0.0, targetBalance));
    }

    public double getReleaseCoins() {
        return releaseCoins;
    }

    public void setReleaseCoins(double releaseCoins) {
        this.releaseCoins = FinancialCalculator.round(Math.max(0.0, releaseCoins));
    }

    /**
     * Total virtual balance held in the user's wallet, including both available and locked funds.
     */
    public double getTotalBalance() {
        return FinancialCalculator.calculateTotalBalance(availableBalance, lockedSavings);
    }

    public double getTotalWealth() {
        return getTotalBalance();
    }

    /**
     * Whether the user has achieved their savings target.
     */
    public boolean isTargetCompleted() {
        return FinancialCalculator.isTargetCompleted(getTotalBalance(), targetBalance);
    }

    /**
     * Effective Balance: The amount of existing wallet balance currently available to spend or withdraw.
     *
     * Core Rule (Prompt 4, Section 2):
     * - When savings target is completed, all committed savings are unlocked: Effective Balance = Total Balance.
     * - While savings target is incomplete, all funds committed toward the target are locked.
     *   Release Coins represent unlocking capacity (1 Coin = ₹1.00).
     *   Therefore, the withdrawable amount can NEVER exceed the user's Release Coins:
     *   Effective Balance = min(Total Balance, Release Coins).
     *   Locked Savings = max(0, Total Balance - Release Coins).
     */
    public double getEffectiveBalance() {
        if (isTargetCompleted()) {
            return getTotalBalance();
        }
        return FinancialCalculator.calculateEffectiveBalanceUnderCommitment(
                getTotalBalance(), releaseCoins, targetBalance);
    }

    /**
     * The remaining locked savings after Release Coins have cancelled their eligible portion of the lockup.
     * Invariant: getEffectiveBalance() + getEffectiveLockedSavings() == getTotalBalance().
     */
    public double getEffectiveLockedSavings() {
        if (isTargetCompleted()) {
            return 0.0;
        }
        return FinancialCalculator.calculateLockedSavings(
                getTotalBalance(), releaseCoins, targetBalance);
    }

    public double getWithdrawableAmount() {
        return getEffectiveBalance();
    }

    /**
     * Uniform hint text shown across the app indicating the exact amount that can be withdrawn.
     */
    public String getWithdrawableHint() {
        double effective = getEffectiveBalance();
        double locked = getEffectiveLockedSavings();
        double total = getTotalBalance();
        if (isTargetCompleted()) {
            return String.format(Locale.getDefault(),
                    "💡 Target Achieved! Full balance of ₹%.2f can be withdrawn.", total);
        } else if (effective > 0) {
            return String.format(Locale.getDefault(),
                    "💡 ₹%.2f can be withdrawn (unlocked by %.0f Release Coins; Locked: ₹%.2f)",
                    effective, releaseCoins, locked);
        } else {
            return String.format(Locale.getDefault(),
                    "💡 Withdrawable: ₹0.00 (₹%.2f locked toward target of ₹%.2f)",
                    locked, targetBalance);
        }
    }

    public int getProgressPercentage() {
        if (targetBalance <= 0.0) return 100;
        return (int) Math.min(100, Math.round((getTotalBalance() / targetBalance) * 100.0));
    }
}
