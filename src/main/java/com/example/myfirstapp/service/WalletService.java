package com.example.myfirstapp.service;

import com.example.myfirstapp.dao.ApprovalDao;
import com.example.myfirstapp.dao.TransactionDao;
import com.example.myfirstapp.dao.UserDao;
import com.example.myfirstapp.dao.WalletDao;
import com.example.myfirstapp.model.ApprovalRequest;
import com.example.myfirstapp.model.Transaction;
import com.example.myfirstapp.model.User;
import com.example.myfirstapp.model.Wallet;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;

public class WalletService {
    public static class TransferResult {
        private final boolean success;
        private final String message;
        private final Transaction transaction;

        public TransferResult(boolean success, String message, Transaction transaction) {
            this.success = success;
            this.message = message;
            this.transaction = transaction;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Transaction getTransaction() {
            return transaction;
        }
    }

    public static class RedemptionResult {
        private final boolean success;
        private final String message;
        private final double coinsRedeemed;
        private final double amountUnlocked;
        private final double resultingAvailable;
        private final double resultingLocked;
        private final double totalBalance;

        public RedemptionResult(boolean success, String message, double coinsRedeemed,
                                double amountUnlocked, double resultingAvailable,
                                double resultingLocked, double totalBalance) {
            this.success = success;
            this.message = message;
            this.coinsRedeemed = coinsRedeemed;
            this.amountUnlocked = amountUnlocked;
            this.resultingAvailable = resultingAvailable;
            this.resultingLocked = resultingLocked;
            this.totalBalance = totalBalance;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public double getCoinsRedeemed() { return coinsRedeemed; }
        public double getAmountUnlocked() { return amountUnlocked; }
        public double getResultingAvailable() { return resultingAvailable; }
        public double getResultingLocked() { return resultingLocked; }
        public double getTotalBalance() { return totalBalance; }
    }

    private final WalletDao walletDao;
    private final UserDao userDao;
    private final TransactionDao transactionDao;
    private final ApprovalDao approvalDao;

    public WalletService() {
        this.walletDao = new WalletDao();
        this.userDao = new UserDao();
        this.transactionDao = new TransactionDao();
        this.approvalDao = new ApprovalDao();
    }

    public WalletService(WalletDao walletDao, UserDao userDao, TransactionDao transactionDao) {
        this.walletDao = walletDao;
        this.userDao = userDao;
        this.transactionDao = transactionDao;
        this.approvalDao = new ApprovalDao();
    }

    public WalletService(WalletDao walletDao, UserDao userDao, TransactionDao transactionDao, ApprovalDao approvalDao) {
        this.walletDao = walletDao;
        this.userDao = userDao;
        this.transactionDao = transactionDao;
        this.approvalDao = approvalDao;
    }

    public Wallet getWallet(int userId) {
        return walletDao.findByUserId(userId).orElseGet(() -> {
            Wallet w = new Wallet(userId, 100.50, 50.00, 200.00, 50.00);
            try {
                walletDao.create(w);
            } catch (SQLException ignored) {
            }
            return w;
        });
    }

    /**
     * P2P virtual funds transfer between registered users with comprehensive validation.
     * Enforces that only the Effective Balance can be transferred.
     */
    public synchronized TransferResult transferFunds(int senderId, String recipientIdentifier, double amount, String description) {
        // 1. Validate amount
        if (amount <= 0.0) {
            return new TransferResult(false, "Transfer amount must be strictly greater than ₹0.00", null);
        }

        // 2. Resolve recipient
        if (recipientIdentifier == null || recipientIdentifier.trim().isEmpty()) {
            return new TransferResult(false, "Recipient details cannot be empty", null);
        }

        Optional<User> recipientOpt = userDao.findByPhoneOrEmailOrUsername(recipientIdentifier.trim());
        if (recipientOpt.isEmpty()) {
            return new TransferResult(false, "Recipient '" + recipientIdentifier.trim() + "' is not a registered user", null);
        }

        User recipient = recipientOpt.get();
        if (recipient.getId() == senderId) {
            return new TransferResult(false, "Self-transfers are not allowed", null);
        }

        // 3. Check sender wallet and Available Balance (Locked savings are NEVER spendable through ordinary transfers!)
        Wallet senderWallet = getWallet(senderId);
        double available = senderWallet.getAvailableBalance();
        if (available < amount) {
            return new TransferResult(false, String.format(
                    "Insufficient available balance! Available: ₹%.2f (Locked savings: ₹%.2f cannot be spent)",
                    available, senderWallet.getLockedSavings()), null);
        }

        // 4. Execute atomic transfer
        try {
            boolean success = walletDao.transferFunds(senderId, recipient.getId(), amount, description);
            if (success) {
                Transaction tx = new Transaction(0, "", senderId, recipient.getId(), amount,
                        Transaction.Type.TRANSFER, Transaction.Status.SUCCESS, description, null);
                return new TransferResult(true, String.format("Successfully transferred ₹%.2f to %s (@%s)",
                        amount, recipient.getFullName(), recipient.getUsername()), tx);
            } else {
                return new TransferResult(false, "Transaction failed due to concurrent modification or balance check", null);
            }
        } catch (SQLException e) {
            return new TransferResult(false, "Database error during transfer: " + e.getMessage(), null);
        }
    }

    /**
     * Simulated virtual deposit (parent-to-child or virtual top-up).
     */
    public synchronized boolean depositVirtualFunds(int userId, double amount, String description) {
        if (amount <= 0.0) return false;
        Wallet wallet = getWallet(userId);
        wallet.setAvailableBalance(wallet.getAvailableBalance() + amount);
        try {
            walletDao.update(wallet);
            transactionDao.record(null, userId, amount, Transaction.Type.DEPOSIT,
                    Transaction.Status.SUCCESS, description != null ? description : "Virtual Cash Deposit");
            return true;
        } catch (SQLException e) {
            System.err.println("Deposit error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Withdraws funds from Effective Balance when authorized.
     * Enforces:
     * - Release Coins are spent when withdrawing under an incomplete savings target.
     * - Balance (available and locked) is updated accurately and consistently.
     * - Invariants: Total balance decreases by exactly the withdrawal amount;
     *   coins decrease by the amount of coins spent to unlock the withdrawal.
     */
    public synchronized boolean withdrawFunds(int userId, double amount, boolean authorized) {
        if (amount <= 0.0 || !authorized) return false;
        Wallet wallet = getWallet(userId);
        if (wallet.getEffectiveBalance() < amount) return false;

        // 1. Calculate and deduct from available balance vs locked savings
        double fromAvailable = Math.min(wallet.getAvailableBalance(), amount);
        double fromLocked = FinancialCalculator.round(amount - fromAvailable);

        wallet.setAvailableBalance(FinancialCalculator.round(wallet.getAvailableBalance() - fromAvailable));
        wallet.setLockedSavings(FinancialCalculator.round(wallet.getLockedSavings() - fromLocked));

        // 2. Spend Release Coins:
        // While target is incomplete, Release Coins provide the early-unlocking capacity (1 Coin = ₹1.00).
        // Coins used to enable this withdrawal are spent and deducted from the user's coin count.
        double coinsToSpend = 0.0;
        if (!wallet.isTargetCompleted() && wallet.getReleaseCoins() > 0) {
            coinsToSpend = FinancialCalculator.round(Math.min(wallet.getReleaseCoins(), amount));
            wallet.setReleaseCoins(FinancialCalculator.round(wallet.getReleaseCoins() - coinsToSpend));
        }

        try {
            walletDao.update(wallet);

            if (coinsToSpend > 0) {
                transactionDao.record(userId, null, coinsToSpend, Transaction.Type.COIN_REDEEM,
                        Transaction.Status.SUCCESS, String.format(Locale.getDefault(),
                                "Spent %.0f Release Coins to unlock early withdrawal of ₹%.2f", coinsToSpend, amount));
            }

            transactionDao.record(userId, null, amount, Transaction.Type.WITHDRAWAL,
                    Transaction.Status.SUCCESS, "Virtual Wallet Withdrawal");
            return true;
        } catch (SQLException e) {
            System.err.println("Withdrawal error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lock available funds into locked savings discipline vault.
     */
    public synchronized boolean lockFundsIntoSavings(int userId, double amount) {
        if (amount <= 0.0) return false;
        Wallet wallet = getWallet(userId);
        if (wallet.getAvailableBalance() < amount) return false;

        wallet.setAvailableBalance(wallet.getAvailableBalance() - amount);
        wallet.setLockedSavings(wallet.getLockedSavings() + amount);
        try {
            walletDao.update(wallet);
            transactionDao.record(userId, userId, amount, Transaction.Type.LOCK_SAVINGS,
                    Transaction.Status.SUCCESS, "Locked into Savings Goal");
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Unlock savings when goal achieved or early release approved.
     */
    public synchronized boolean unlockSavings(int userId, double amount) {
        if (amount <= 0.0) return false;
        Wallet wallet = getWallet(userId);
        if (wallet.getLockedSavings() < amount) return false;

        wallet.setLockedSavings(wallet.getLockedSavings() - amount);
        wallet.setAvailableBalance(wallet.getAvailableBalance() + amount);
        try {
            walletDao.update(wallet);
            transactionDao.record(userId, userId, amount, Transaction.Type.UNLOCK_SAVINGS,
                    Transaction.Status.SUCCESS, "Savings Unlocked to Available Balance");
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Core Release Coin Redemption:
     * 1. Validate that the user has enough unused Release Coins.
     * 2. Calculate the amount of savings that can be unlocked using FinancialCalculator.
     * 3. Reduce the user's Release Coin count by the number actually redeemed.
     * 4. Increase the user's available balance by unlocking the corresponding amount of existing savings.
     * 5. Do NOT create new money or increase the total wallet balance.
     * 6. Record the redemption and unlocking transaction persistently.
     */
    public synchronized RedemptionResult redeemCoinsToUnlockSavings(int userId, double coinsToRedeem) {
        if (coinsToRedeem <= 0.0) {
            return new RedemptionResult(false, "Redemption amount must be strictly greater than 0 coins.",
                    0, 0, 0, 0, 0);
        }

        Wallet wallet = getWallet(userId);
        if (wallet.getReleaseCoins() < coinsToRedeem) {
            return new RedemptionResult(false, String.format(
                    "Insufficient Release Coins! Requested: %.0f, Available: %.0f",
                    coinsToRedeem, wallet.getReleaseCoins()),
                    0, 0, wallet.getAvailableBalance(), wallet.getLockedSavings(), wallet.getTotalBalance());
        }

        double lockedSavings = wallet.getLockedSavings();
        if (lockedSavings <= 0.0) {
            return new RedemptionResult(false,
                    "No locked savings to unlock! All funds are already available or target is achieved.",
                    0, 0, wallet.getAvailableBalance(), wallet.getLockedSavings(), wallet.getTotalBalance());
        }

        // Calculate amount eligible to unlock (1 Coin = ₹1 virtual value)
        double unlockable = FinancialCalculator.calculateUnlockableAmount(
                lockedSavings, wallet.getReleaseCoins(), coinsToRedeem);

        if (unlockable <= 0.0) {
            return new RedemptionResult(false, "No savings could be unlocked with the specified coin amount.",
                    0, 0, wallet.getAvailableBalance(), wallet.getLockedSavings(), wallet.getTotalBalance());
        }

        // Actual coins deducted matches the amount unlocked (1 coin per ₹1 unlocked)
        double actualCoinsRedeemed = unlockable;
        double initialTotal = wallet.getTotalBalance();

        // Update balances: shift from locked to available
        wallet.setReleaseCoins(wallet.getReleaseCoins() - actualCoinsRedeemed);
        wallet.setLockedSavings(wallet.getLockedSavings() - unlockable);
        wallet.setAvailableBalance(wallet.getAvailableBalance() + unlockable);

        // Invariant verification: Total balance must remain IDENTICAL (no new money created)
        assert Math.abs(wallet.getTotalBalance() - initialTotal) < 0.001 : "Total balance invariant violated!";

        try {
            walletDao.update(wallet);
            transactionDao.record(userId, userId, unlockable, Transaction.Type.UNLOCK_SAVINGS,
                    Transaction.Status.SUCCESS, String.format("Unlocked ₹%.2f savings by redeeming %.0f Release Coins",
                            unlockable, actualCoinsRedeemed));
            return new RedemptionResult(true, String.format(
                    "Successfully redeemed %.0f Release Coins and unlocked ₹%.2f of savings!",
                    actualCoinsRedeemed, unlockable),
                    actualCoinsRedeemed, unlockable,
                    wallet.getAvailableBalance(), wallet.getLockedSavings(), wallet.getTotalBalance());
        } catch (SQLException e) {
            return new RedemptionResult(false, "Database error during redemption: " + e.getMessage(),
                    0, 0, wallet.getAvailableBalance(), wallet.getLockedSavings(), wallet.getTotalBalance());
        }
    }

    /**
     * Updates savings target and recalculates locked and available balances consistently.
     * Enforces the rule: Target cannot be changed without Guardian permission.
     *
     * @param userId user id
     * @param newTarget new savings target amount
     * @param guardianApproved whether a registered guardian has authorized this target change
     */
    public synchronized boolean updateSavingsTarget(int userId, double newTarget, boolean guardianApproved) {
        if (newTarget < 0.0 || !guardianApproved) {
            return false;
        }
        Wallet wallet = getWallet(userId);

        FinancialCalculator.TargetRecalculationResult result =
                FinancialCalculator.recalculateOnTargetChange(
                        wallet.getAvailableBalance(), wallet.getLockedSavings(),
                        wallet.getTargetBalance(), newTarget);

        wallet.setTargetBalance(result.getTargetBalance());
        wallet.setAvailableBalance(result.getAvailableBalance());
        wallet.setLockedSavings(result.getLockedSavings());

        try {
            walletDao.update(wallet);
            if (result.isTargetCompleted()) {
                transactionDao.record(userId, userId, result.getTotalBalance(), Transaction.Type.UNLOCK_SAVINGS,
                        Transaction.Status.SUCCESS, "Savings Target Achieved! All funds unlocked.");
            }
            transactionDao.record(userId, null, newTarget, Transaction.Type.UNLOCK_SAVINGS,
                    Transaction.Status.SUCCESS, "Target updated to ₹" + newTarget + " with Guardian permission");
            return true;
        } catch (SQLException e) {
            System.err.println("Error updating savings target: " + e.getMessage());
            return false;
        }
    }

    /**
     * Overload checking persistent Guardian approval in the approvals table.
     */
    public synchronized boolean updateSavingsTarget(int userId, double newTarget) {
        boolean guardianApproved = isGuardianTargetPermissionGranted(userId);
        return updateSavingsTarget(userId, newTarget, guardianApproved);
    }

    public synchronized boolean setGuardianTargetPermission(int userId, boolean granted, String guardianName) {
        approvalDao.setApproval(userId, ApprovalRequest.Type.TARGET_CHANGE,
                granted ? ApprovalRequest.Status.APPROVED : ApprovalRequest.Status.REJECTED,
                "Guardian permission for target change: " + guardianName,
                granted ? "GRANTED" : "REVOKED");
        return true;
    }

    public synchronized boolean isGuardianTargetPermissionGranted(int userId) {
        return approvalDao.isApproved(userId, ApprovalRequest.Type.TARGET_CHANGE);
    }
}
