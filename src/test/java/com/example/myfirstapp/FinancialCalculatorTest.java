package com.example.myfirstapp;

import com.example.myfirstapp.dao.DatabaseManager;
import com.example.myfirstapp.dao.TransactionDao;
import com.example.myfirstapp.dao.UserDao;
import com.example.myfirstapp.dao.WalletDao;
import com.example.myfirstapp.model.User;
import com.example.myfirstapp.model.Wallet;
import com.example.myfirstapp.service.FinancialCalculator;
import com.example.myfirstapp.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class FinancialCalculatorTest {

    private DatabaseManager dbManager;
    private WalletDao walletDao;
    private UserDao userDao;
    private TransactionDao transactionDao;
    private WalletService walletService;

    private User admin;
    private User sarah;

    @BeforeEach
    void setUp() throws SQLException {
        String testDbUrl = "jdbc:sqlite:test_fincalc_" + UUID.randomUUID() + ".db";
        DatabaseManager.resetWithUrl(testDbUrl);
        dbManager = DatabaseManager.getInstance();

        userDao = new UserDao(dbManager);
        walletDao = new WalletDao(dbManager);
        transactionDao = new TransactionDao(dbManager);
        walletService = new WalletService(walletDao, userDao, transactionDao);

        admin = userDao.findByUsername("admin").orElseThrow();
        sarah = userDao.findByUsername("sarah").orElseThrow();
    }

    @Test
    @DisplayName("Zero balance and zero Release Coins")
    void testZeroBalanceAndZeroCoins() {
        double totalBalance = 0.0;
        double releaseCoins = 0.0;
        double target = 100.0;

        double effectiveBalance = FinancialCalculator.calculateEffectiveBalanceUnderCommitment(totalBalance, releaseCoins, target);
        double lockedSavings = FinancialCalculator.calculateLockedSavings(totalBalance, releaseCoins, target);
        double unlockable = FinancialCalculator.calculateUnlockableAmount(0.0, releaseCoins, 10.0);

        assertEquals(0.0, effectiveBalance, 0.001, "Effective balance must be 0 for zero balance");
        assertEquals(0.0, lockedSavings, 0.001, "Locked savings must be 0 for zero balance");
        assertEquals(0.0, unlockable, 0.001, "Unlockable amount must be 0 for zero coins/locked savings");
        assertFalse(FinancialCalculator.isTargetCompleted(totalBalance, target), "Target should not be completed");
    }

    @Test
    @DisplayName("Balance greater than Release Coins")
    void testBalanceGreaterThanReleaseCoins() {
        // Total Balance = ₹150, Release Coins = 50, Target = ₹200 (incomplete target)
        double totalBalance = 150.0;
        double releaseCoins = 50.0;
        double target = 200.0;

        double effectiveBalance = FinancialCalculator.calculateEffectiveBalanceUnderCommitment(totalBalance, releaseCoins, target);
        double lockedSavings = FinancialCalculator.calculateLockedSavings(totalBalance, releaseCoins, target);

        // Effective balance is min(Balance, Release Coins) = 50.0
        assertEquals(50.0, effectiveBalance, 0.001, "Effective balance should be min(Balance, Coins) = 50.0");
        // Locked savings is max(0, Balance - Release Coins) = 100.0
        assertEquals(100.0, lockedSavings, 0.001, "Locked savings should be max(0, Balance - Coins) = 100.0");

        // Release Coins are unlocking capacity, NOT money subtracted from balance
        assertEquals(totalBalance, effectiveBalance + lockedSavings, 0.001,
                "Effective balance + Locked savings must equal Total Balance (Release Coins are capacity, not deductions)");
    }

    @Test
    @DisplayName("Release Coins greater than Balance")
    void testReleaseCoinsGreaterThanBalance() {
        // Total Balance = ₹50, Release Coins = 120, Target = ₹100
        double totalBalance = 50.0;
        double releaseCoins = 120.0;
        double target = 100.0;

        double effectiveBalance = FinancialCalculator.calculateEffectiveBalanceUnderCommitment(totalBalance, releaseCoins, target);
        double lockedSavings = FinancialCalculator.calculateLockedSavings(totalBalance, releaseCoins, target);

        // Effective balance cannot exceed actual wallet balance
        assertEquals(50.0, effectiveBalance, 0.001, "Effective balance must never exceed actual wallet balance");
        assertEquals(0.0, lockedSavings, 0.001, "Locked savings should be 0 since coins unlock all available balance");
        assertTrue(effectiveBalance <= totalBalance, "Available amount must never exceed actual balance");
    }

    @Test
    @DisplayName("Partial redemption of Release Coins")
    void testPartialRedemptionOfReleaseCoins() throws SQLException {
        // Set admin to: ₹40 available, ₹80 locked (Total ₹120), and 100 Release Coins
        Wallet wallet = walletService.getWallet(admin.getId());
        wallet.setAvailableBalance(40.0);
        wallet.setLockedSavings(80.0);
        wallet.setTargetBalance(200.0);
        wallet.setReleaseCoins(100.0);
        walletDao.update(wallet);

        // Redeem 20 coins
        WalletService.RedemptionResult result = walletService.redeemCoinsToUnlockSavings(admin.getId(), 20.0);
        assertTrue(result.isSuccess(), "Redemption of 20 coins should succeed");
        assertEquals(20.0, result.getCoinsRedeemed(), 0.001);
        assertEquals(20.0, result.getAmountUnlocked(), 0.001);

        Wallet updated = walletService.getWallet(admin.getId());
        // Coins reduced by 20 (100 -> 80)
        assertEquals(80.0, updated.getReleaseCoins(), 0.001, "Coins should be reduced by 20");
        // Locked savings reduced by ₹20 (80 -> 60)
        assertEquals(60.0, updated.getLockedSavings(), 0.001, "Locked savings should decrease by ₹20");
        // Available balance increased by ₹20 (40 -> 60)
        assertEquals(60.0, updated.getAvailableBalance(), 0.001, "Available balance should increase by ₹20");
        // Total balance unchanged (no new money created)
        assertEquals(120.0, updated.getTotalBalance(), 0.001, "Total balance must remain strictly invariant at ₹120");
    }

    @Test
    @DisplayName("Completed and incomplete savings targets")
    void testCompletedAndIncompleteSavingsTargets() {
        // Incomplete target: Balance ₹80 < Target ₹100
        assertFalse(FinancialCalculator.isTargetCompleted(80.0, 100.0));
        assertEquals(0.0, FinancialCalculator.calculateEffectiveBalanceUnderCommitment(80.0, 0.0, 100.0), 0.001);
        assertEquals(80.0, FinancialCalculator.calculateLockedSavings(80.0, 0.0, 100.0), 0.001);

        // Completed target: Balance ₹100 >= Target ₹100
        assertTrue(FinancialCalculator.isTargetCompleted(100.0, 100.0));
        assertEquals(100.0, FinancialCalculator.calculateEffectiveBalanceUnderCommitment(100.0, 0.0, 100.0), 0.001,
                "When target is completed, all committed savings are unlocked");
        assertEquals(0.0, FinancialCalculator.calculateLockedSavings(100.0, 0.0, 100.0), 0.001,
                "When target is completed, locked savings should be 0");

        // Exceeded target: Balance ₹150 >= Target ₹100
        assertTrue(FinancialCalculator.isTargetCompleted(150.0, 100.0));
        assertEquals(150.0, FinancialCalculator.calculateEffectiveBalanceUnderCommitment(150.0, 0.0, 100.0), 0.001);
        assertEquals(0.0, FinancialCalculator.calculateLockedSavings(150.0, 0.0, 100.0), 0.001);
    }

    @Test
    @DisplayName("Target increase recalculates consistently")
    void testTargetIncreaseRecalculatesConsistently() {
        // User had ₹150 total (Target was ₹100 - completed, so ₹150 available, ₹0 locked)
        double currentAvailable = 150.0;
        double currentLocked = 0.0;
        double currentTarget = 100.0;

        // User increases target to ₹250.0
        double newTarget = 250.0;
        FinancialCalculator.TargetRecalculationResult result =
                FinancialCalculator.recalculateOnTargetChange(currentAvailable, currentLocked, currentTarget, newTarget);

        assertFalse(result.isTargetCompleted(), "New target of ₹250 is incomplete since total is ₹150");
        assertEquals(150.0, result.getTotalBalance(), 0.001, "Total balance must remain ₹150");
        assertEquals(150.0, result.getLockedSavings(), 0.001, "Committed savings up to total balance should be locked");
        assertEquals(0.0, result.getAvailableBalance(), 0.001, "Available balance should be 0 until target or unlocked");
        assertEquals(150.0, result.getAvailableBalance() + result.getLockedSavings(), 0.001,
                "Sum of available and locked must equal total balance");
    }

    @Test
    @DisplayName("Insufficient available balance prevents transfer")
    void testInsufficientAvailableBalance() throws SQLException {
        // Admin has ₹30 available, ₹70 locked (Total ₹100)
        Wallet adminWallet = walletService.getWallet(admin.getId());
        adminWallet.setAvailableBalance(30.0);
        adminWallet.setLockedSavings(70.0);
        adminWallet.setTargetBalance(200.0);
        adminWallet.setReleaseCoins(0.0);
        walletDao.update(adminWallet);

        // Sarah has ₹50 available
        Wallet sarahWallet = walletService.getWallet(sarah.getId());
        sarahWallet.setAvailableBalance(50.0);
        walletDao.update(sarahWallet);

        // Attempt transfer of ₹50 (exceeds available ₹30 even though total is ₹100)
        WalletService.TransferResult result = walletService.transferFunds(admin.getId(), sarah.getUsername(), 50.0, "Test Transfer");
        assertFalse(result.isSuccess(), "Transfer exceeding available balance must fail");
        assertTrue(result.getMessage().contains("Insufficient available balance"), "Should explain insufficient available balance");

        // Verify balances unchanged
        Wallet verifiedAdmin = walletService.getWallet(admin.getId());
        assertEquals(30.0, verifiedAdmin.getAvailableBalance(), 0.001, "Sender available balance must remain unchanged");
        assertEquals(70.0, verifiedAdmin.getLockedSavings(), 0.001, "Sender locked savings must remain unchanged");
    }

    @Test
    @DisplayName("Duplicate redemption attempts prevented")
    void testDuplicateRedemptionAttempts() throws SQLException {
        // Admin with only 10 Release Coins and ₹50 locked savings
        Wallet wallet = walletService.getWallet(admin.getId());
        wallet.setAvailableBalance(20.0);
        wallet.setLockedSavings(50.0);
        wallet.setTargetBalance(200.0);
        wallet.setReleaseCoins(10.0);
        walletDao.update(wallet);

        // First redemption of 10 coins: succeeds
        WalletService.RedemptionResult result1 = walletService.redeemCoinsToUnlockSavings(admin.getId(), 10.0);
        assertTrue(result1.isSuccess(), "First redemption should succeed");
        assertEquals(10.0, result1.getCoinsRedeemed(), 0.001);

        // Immediate duplicate redemption attempt of 10 coins: must fail (0 coins left)
        WalletService.RedemptionResult result2 = walletService.redeemCoinsToUnlockSavings(admin.getId(), 10.0);
        assertFalse(result2.isSuccess(), "Second redemption should fail due to exhausted coins");
        assertTrue(result2.getMessage().contains("Insufficient Release Coins"), "Should report insufficient coins");
    }

    @Test
    @DisplayName("Preventing negative balances and double spending")
    void testPreventingNegativeBalancesAndDoubleSpending() throws SQLException {
        Wallet wallet = walletService.getWallet(admin.getId());
        wallet.setAvailableBalance(50.0);
        wallet.setLockedSavings(50.0);
        wallet.setTargetBalance(200.0);
        wallet.setReleaseCoins(20.0);
        walletDao.update(wallet);

        // 1. Negative transfer amount rejected
        WalletService.TransferResult negTransfer = walletService.transferFunds(admin.getId(), sarah.getUsername(), -25.0, "Negative transfer");
        assertFalse(negTransfer.isSuccess(), "Negative transfer must be rejected");

        // 2. Negative coin redemption rejected
        WalletService.RedemptionResult negRedeem = walletService.redeemCoinsToUnlockSavings(admin.getId(), -10.0);
        assertFalse(negRedeem.isSuccess(), "Negative coin redemption must be rejected");

        // 3. Negative withdrawal rejected
        boolean negWithdraw = walletService.withdrawFunds(admin.getId(), -15.0, true);
        assertFalse(negWithdraw, "Negative withdrawal must be rejected");

        // 4. Overdrawing withdrawal rejected
        boolean overWithdraw = walletService.withdrawFunds(admin.getId(), 60.0, true);
        assertFalse(overWithdraw, "Withdrawal exceeding available balance must be rejected");

        // Invariant: Wallet balances remain positive and sum remains constant
        Wallet verified = walletService.getWallet(admin.getId());
        assertTrue(verified.getAvailableBalance() >= 0.0, "Available balance must never be negative");
        assertTrue(verified.getLockedSavings() >= 0.0, "Locked savings must never be negative");
        assertEquals(100.0, verified.getTotalBalance(), 0.001, "Total balance must remain 100.0");
    }
}
