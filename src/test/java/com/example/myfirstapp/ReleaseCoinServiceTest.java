package com.example.myfirstapp;

import com.example.myfirstapp.dao.AdEventDao;
import com.example.myfirstapp.dao.DatabaseManager;
import com.example.myfirstapp.dao.TransactionDao;
import com.example.myfirstapp.dao.UserDao;
import com.example.myfirstapp.dao.WalletDao;
import com.example.myfirstapp.model.User;
import com.example.myfirstapp.model.Wallet;
import com.example.myfirstapp.service.ReleaseCoinService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReleaseCoinServiceTest {
    private DatabaseManager dbManager;
    private ReleaseCoinService releaseCoinService;
    private WalletDao walletDao;
    private UserDao userDao;
    private User testUser;
    private User friendUser;

    @BeforeEach
    void setUp() {
        String testDbUrl = "jdbc:sqlite:test_coins_" + UUID.randomUUID() + ".db";
        DatabaseManager.resetWithUrl(testDbUrl);
        dbManager = DatabaseManager.getInstance();

        walletDao = new WalletDao(dbManager);
        userDao = new UserDao(dbManager);
        AdEventDao adEventDao = new AdEventDao(dbManager);
        TransactionDao transactionDao = new TransactionDao(dbManager);

        releaseCoinService = new ReleaseCoinService(walletDao, adEventDao, userDao, transactionDao);

        testUser = userDao.findByUsername("admin").orElseThrow();
        friendUser = userDao.findByUsername("sarah").orElseThrow();

        // Baseline coins
        Wallet wallet = walletDao.findByUserId(testUser.getId()).orElseThrow();
        wallet.setReleaseCoins(50.0);
        try {
            walletDao.update(wallet);
        } catch (Exception ignored) {}
    }

    @Test
    @DisplayName("Watching an eligible ad awards up to 5 Release Coins maximum")
    void testAdCoinAwardCap() {
        // Requesting 10 coins should be capped at 5.0 according to rule
        boolean awarded = releaseCoinService.awardAdCoins(testUser.getId(), "AD_CAMPAIGN_001", 10.0);

        assertThat(awarded).isTrue();
        double currentCoins = releaseCoinService.getReleaseCoins(testUser.getId());
        assertThat(currentCoins).isEqualTo(55.0); // 50.0 + 5.0 max cap
    }

    @Test
    @DisplayName("Duplicate rewards must be prevented for the same advertisement event")
    void testDuplicateAdRewardPrevention() {
        String adToken = "UNIQUE_AD_PROMO_2026";

        // First watch succeeds
        boolean firstWatch = releaseCoinService.awardAdCoins(testUser.getId(), adToken, 5.0);
        assertThat(firstWatch).isTrue();

        // Second watch for the SAME ad token must be rejected!
        boolean duplicateWatch = releaseCoinService.awardAdCoins(testUser.getId(), adToken, 5.0);
        assertThat(duplicateWatch).isFalse();

        // Coin balance should only reflect 1 award (55.0, not 60.0)
        assertThat(releaseCoinService.getReleaseCoins(testUser.getId())).isEqualTo(55.0);
    }

    @Test
    @DisplayName("Referral awards +25 coins when code belongs to another registered user")
    void testReferralCoinAward() {
        // friendUser has referral code "SARAH-SAVE-101"
        boolean refSuccess = releaseCoinService.awardReferralCoins(testUser.getId(), "SARAH-SAVE-101");
        assertThat(refSuccess).isTrue();

        assertThat(releaseCoinService.getReleaseCoins(testUser.getId())).isEqualTo(75.0); // 50 + 25

        // Self-referral must be rejected
        boolean selfRef = releaseCoinService.awardReferralCoins(testUser.getId(), testUser.getReferralCode());
        assertThat(selfRef).isFalse();
    }

    @Test
    @DisplayName("Redeeming Release Coins unlocks early release and deducts balance")
    void testRedeemCoinsForEarlyRelease() {
        double coinsToRedeem = 20.0;
        boolean redeemed = releaseCoinService.redeemCoinsForRelease(testUser.getId(), coinsToRedeem);

        assertThat(redeemed).isTrue();
        assertThat(releaseCoinService.getReleaseCoins(testUser.getId())).isEqualTo(30.0); // 50 - 20

        // Trying to redeem more than available balance must fail
        boolean overRedeem = releaseCoinService.redeemCoinsForRelease(testUser.getId(), 500.0);
        assertThat(overRedeem).isFalse();
    }
}
