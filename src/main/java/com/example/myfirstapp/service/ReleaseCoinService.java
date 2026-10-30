package com.example.myfirstapp.service;

import com.example.myfirstapp.dao.AdEventDao;
import com.example.myfirstapp.dao.TransactionDao;
import com.example.myfirstapp.dao.UserDao;
import com.example.myfirstapp.dao.WalletDao;
import com.example.myfirstapp.model.Transaction;
import com.example.myfirstapp.model.User;
import com.example.myfirstapp.model.Wallet;

import java.sql.SQLException;
import java.util.Optional;

public class ReleaseCoinService {
    public static final double MAX_AD_COIN_AWARD = 5.0; // Rule: Up to 5 Release Coins per ad
    public static final double REFERRAL_REWARD_COINS = 25.0;

    private final WalletDao walletDao;
    private final AdEventDao adEventDao;
    private final UserDao userDao;
    private final TransactionDao transactionDao;

    public ReleaseCoinService() {
        this.walletDao = new WalletDao();
        this.adEventDao = new AdEventDao();
        this.userDao = new UserDao();
        this.transactionDao = new TransactionDao();
    }

    public ReleaseCoinService(WalletDao walletDao, AdEventDao adEventDao, UserDao userDao, TransactionDao transactionDao) {
        this.walletDao = walletDao;
        this.adEventDao = adEventDao;
        this.userDao = userDao;
        this.transactionDao = transactionDao;
    }

    public double getReleaseCoins(int userId) {
        return walletDao.findByUserId(userId).map(Wallet::getReleaseCoins).orElse(0.0);
    }

    /**
     * Awards Release Coins for watching an eligible advertisement.
     * Enforces the rule that an eligible ad awards up to 5 coins and prevents duplicate awards.
     */
    public synchronized boolean awardAdCoins(int userId, String adToken, double coinsRequested) {
        if (adToken == null || adToken.isBlank()) {
            return false;
        }

        // Rule: Watching an eligible advertisement can award up to 5 Release Coins
        double awardedCoins = Math.min(coinsRequested, MAX_AD_COIN_AWARD);
        if (awardedCoins <= 0.0) {
            return false;
        }

        // Rule: Coins must not be awarded repeatedly for the same advertisement event
        boolean recorded = adEventDao.recordAdWatch(userId, adToken.trim(), awardedCoins);
        if (!recorded) {
            return false; // Already rewarded for this ad
        }

        Wallet wallet = walletDao.findByUserId(userId).orElse(new Wallet(userId, 0, 0, 200, 0));
        wallet.setReleaseCoins(wallet.getReleaseCoins() + awardedCoins);
        try {
            walletDao.update(wallet);
            transactionDao.record(null, userId, awardedCoins, Transaction.Type.COIN_REWARD,
                    Transaction.Status.SUCCESS, "Ad Reward (Ad: " + adToken + ")");
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Awards referral coins according to defined reward rule (25 coins).
     */
    public synchronized boolean awardReferralCoins(int userId, String referralCode) {
        if (referralCode == null || referralCode.isBlank()) {
            return false;
        }

        Optional<User> refUser = userDao.findByReferralCode(referralCode.trim());
        if (refUser.isEmpty() || refUser.get().getId() == userId) {
            return false; // Invalid or self referral
        }

        Wallet wallet = walletDao.findByUserId(userId).orElse(new Wallet(userId, 0, 0, 200, 0));
        wallet.setReleaseCoins(wallet.getReleaseCoins() + REFERRAL_REWARD_COINS);
        try {
            walletDao.update(wallet);
            transactionDao.record(null, userId, REFERRAL_REWARD_COINS, Transaction.Type.COIN_REWARD,
                    Transaction.Status.SUCCESS, "Referral Bonus (Code: " + referralCode + ")");
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Awards coins for completing an eligible educational task.
     */
    public synchronized boolean awardTaskCoins(int userId, double coins, String taskTitle) {
        if (coins <= 0.0) return false;
        Wallet wallet = walletDao.findByUserId(userId).orElse(new Wallet(userId, 0, 0, 200, 0));
        wallet.setReleaseCoins(wallet.getReleaseCoins() + coins);
        try {
            walletDao.update(wallet);
            transactionDao.record(null, userId, coins, Transaction.Type.COIN_REWARD,
                    Transaction.Status.SUCCESS, "Study Task: " + taskTitle);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Users can redeem available Release Coins to unlock the corresponding permitted amount of savings.
     * 1 Release Coin = ₹1 of virtual withdrawal or spending value unlocking capacity.
     *
     * Invariants enforced:
     * - Coins deducted must not exceed available coins.
     * - Unlocks up to locked savings amount.
     * - Total wallet balance remains constant (no money created or destroyed).
     */
    public synchronized boolean redeemCoinsForRelease(int userId, double coinsToRedeem) {
        if (coinsToRedeem <= 0.0) return false;
        Wallet wallet = walletDao.findByUserId(userId).orElse(null);
        if (wallet == null || wallet.getReleaseCoins() < coinsToRedeem) {
            return false;
        }

        double lockedSavings = wallet.getLockedSavings();
        if (lockedSavings <= 0.0) {
            return false; // Nothing to unlock
        }

        double unlockable = FinancialCalculator.calculateUnlockableAmount(
                lockedSavings, wallet.getReleaseCoins(), coinsToRedeem);
        if (unlockable <= 0.0) {
            return false;
        }

        // 1 Coin unlocks ₹1 of locked savings
        wallet.setReleaseCoins(wallet.getReleaseCoins() - unlockable);
        wallet.setLockedSavings(wallet.getLockedSavings() - unlockable);
        wallet.setAvailableBalance(wallet.getAvailableBalance() + unlockable);

        try {
            walletDao.update(wallet);
            transactionDao.record(userId, null, unlockable, Transaction.Type.COIN_REDEEM,
                    Transaction.Status.SUCCESS, "Redeemed " + unlockable + " Coins for Early Release Authority");
            transactionDao.record(userId, userId, unlockable, Transaction.Type.UNLOCK_SAVINGS,
                    Transaction.Status.SUCCESS, "Unlocked ₹" + unlockable + " from locked savings to available balance");
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}
