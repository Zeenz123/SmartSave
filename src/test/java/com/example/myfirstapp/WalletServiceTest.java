package com.example.myfirstapp;

import com.example.myfirstapp.dao.DatabaseManager;
import com.example.myfirstapp.dao.TransactionDao;
import com.example.myfirstapp.dao.UserDao;
import com.example.myfirstapp.dao.WalletDao;
import com.example.myfirstapp.model.Transaction;
import com.example.myfirstapp.model.User;
import com.example.myfirstapp.model.Wallet;
import com.example.myfirstapp.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WalletServiceTest {
    private DatabaseManager dbManager;
    private WalletService walletService;
    private UserDao userDao;
    private WalletDao walletDao;
    private TransactionDao transactionDao;

    private User sender;
    private User recipient;

    @BeforeEach
    void setUp() throws SQLException {
        // Use an isolated in-memory or distinct test database for each run
        String testDbUrl = "jdbc:sqlite:test_wallet_" + UUID.randomUUID() + ".db";
        DatabaseManager.resetWithUrl(testDbUrl);
        dbManager = DatabaseManager.getInstance();

        userDao = new UserDao(dbManager);
        walletDao = new WalletDao(dbManager);
        transactionDao = new TransactionDao(dbManager);
        walletService = new WalletService(walletDao, userDao, transactionDao);

        // Retrieve the seeded users: admin and sarah
        sender = userDao.findByUsername("admin").orElseThrow();
        recipient = userDao.findByUsername("sarah").orElseThrow();

        // Set baseline balances
        Wallet senderWallet = walletService.getWallet(sender.getId());
        senderWallet.setAvailableBalance(100.50);
        senderWallet.setLockedSavings(50.00);
        walletDao.update(senderWallet);

        Wallet recipientWallet = walletService.getWallet(recipient.getId());
        recipientWallet.setAvailableBalance(50.00);
        walletDao.update(recipientWallet);
    }

    @Test
    @DisplayName("Virtual transfer successfully decreases sender and increases recipient balance")
    void testSuccessfulVirtualTransferBetweenUsers() {
        double transferAmount = 25.50;

        WalletService.TransferResult result = walletService.transferFunds(
                sender.getId(), recipient.getUsername(), transferAmount, "Lunch split");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).contains("Successfully transferred");

        Wallet updatedSender = walletService.getWallet(sender.getId());
        Wallet updatedRecipient = walletService.getWallet(recipient.getId());

        // Verify sender decreased by exact transfer amount: 100.50 - 25.50 = 75.00
        assertThat(updatedSender.getAvailableBalance()).isEqualTo(75.00);

        // Verify recipient increased by exact transfer amount: 50.00 + 25.50 = 75.50
        assertThat(updatedRecipient.getAvailableBalance()).isEqualTo(75.50);

        // Verify persistent transaction record
        List<Transaction> transactions = transactionDao.findByUserId(sender.getId());
        assertThat(transactions).isNotEmpty();
        assertThat(transactions.get(0).getAmount()).isEqualTo(transferAmount);
        assertThat(transactions.get(0).getType()).isEqualTo(Transaction.Type.TRANSFER);
        assertThat(transactions.get(0).getStatus()).isEqualTo(Transaction.Status.SUCCESS);
    }

    @Test
    @DisplayName("Transfers exceeding sender available balance must be rejected")
    void testTransferExceedingAvailableBalanceRejected() {
        double excessiveAmount = 200.00; // Sender only has 100.50

        WalletService.TransferResult result = walletService.transferFunds(
                sender.getId(), recipient.getUsername(), excessiveAmount, "Overdraft attempt");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Insufficient available balance");

        // Balances remain completely unchanged
        assertThat(walletService.getWallet(sender.getId()).getAvailableBalance()).isEqualTo(100.50);
        assertThat(walletService.getWallet(recipient.getId()).getAvailableBalance()).isEqualTo(50.00);
    }

    @Test
    @DisplayName("Negative or zero transfer amounts must be rejected")
    void testNegativeOrZeroTransferAmountRejected() {
        WalletService.TransferResult resultNegative = walletService.transferFunds(
                sender.getId(), recipient.getUsername(), -10.00, "Negative amount");
        assertThat(resultNegative.isSuccess()).isFalse();
        assertThat(resultNegative.getMessage()).contains("strictly greater than ₹0.00");

        WalletService.TransferResult resultZero = walletService.transferFunds(
                sender.getId(), recipient.getUsername(), 0.00, "Zero amount");
        assertThat(resultZero.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("Self-transfers must be prevented")
    void testSelfTransferRejected() {
        WalletService.TransferResult result = walletService.transferFunds(
                sender.getId(), sender.getUsername(), 10.00, "Self transfer");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Self-transfers are not allowed");
    }

    @Test
    @DisplayName("Transfers to non-existent users must be rejected")
    void testInvalidRecipientRejected() {
        WalletService.TransferResult result = walletService.transferFunds(
                sender.getId(), "non_existent_ghost_user_999", 10.00, "Unknown recipient");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("not a registered user");
    }

    @Test
    @DisplayName("Locked savings must not be spendable through ordinary transfers")
    void testLockedSavingsAreNotSpendable() throws SQLException {
        // Set available balance to 15.00, locked savings to 100.00
        Wallet senderWallet = walletService.getWallet(sender.getId());
        senderWallet.setAvailableBalance(15.00);
        senderWallet.setLockedSavings(100.00);
        senderWallet.setReleaseCoins(0.0);
        walletDao.update(senderWallet);

        // Attempt to transfer $20.00 (exceeds 15.00 available even though total wealth is 115.00)
        WalletService.TransferResult result = walletService.transferFunds(
                sender.getId(), recipient.getUsername(), 20.00, "Trying to spend locked savings");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Locked savings: ₹100.00 cannot be spent");
        assertThat(walletService.getWallet(sender.getId()).getAvailableBalance()).isEqualTo(15.00);
        assertThat(walletService.getWallet(sender.getId()).getLockedSavings()).isEqualTo(100.00);
    }

    @Test
    @DisplayName("Virtual deposit increases available balance and records transaction")
    void testDepositVirtualFunds() {
        boolean depositSuccess = walletService.depositVirtualFunds(sender.getId(), 50.00, "Parent allowance");
        assertThat(depositSuccess).isTrue();

        Wallet updated = walletService.getWallet(sender.getId());
        assertThat(updated.getAvailableBalance()).isEqualTo(150.50);
    }

    @Test
    @DisplayName("Locking funds moves them from available balance to locked savings")
    void testLockFundsIntoSavings() {
        boolean locked = walletService.lockFundsIntoSavings(sender.getId(), 30.00);
        assertThat(locked).isTrue();

        Wallet wallet = walletService.getWallet(sender.getId());
        assertThat(wallet.getAvailableBalance()).isEqualTo(70.50);
        assertThat(wallet.getLockedSavings()).isEqualTo(80.00);
    }

    @Test
    @DisplayName("Withdrawing funds spends Release Coins and updates wallet balances accurately")
    void testWithdrawFundsSpendsCoinsAndUpdatesBalance() throws SQLException {
        // Setup sender: total 115.00 (85 available, 30 locked), target 200.00, coins 85
        Wallet senderWallet = walletService.getWallet(sender.getId());
        senderWallet.setAvailableBalance(85.00);
        senderWallet.setLockedSavings(30.00);
        senderWallet.setTargetBalance(200.00);
        senderWallet.setReleaseCoins(85.00);
        walletDao.update(senderWallet);

        // Withdraw 65.00
        boolean withdrawn = walletService.withdrawFunds(sender.getId(), 65.00, true);
        assertThat(withdrawn).isTrue();

        Wallet updated = walletService.getWallet(sender.getId());
        // Verify coins spent: 85 - 65 = 20
        assertThat(updated.getReleaseCoins()).isEqualTo(20.00);
        // Verify available balance decreased: 85 - 65 = 20
        assertThat(updated.getAvailableBalance()).isEqualTo(20.00);
        // Verify locked savings remained constant: 30
        assertThat(updated.getLockedSavings()).isEqualTo(30.00);
        // Verify total balance decreased by exact amount: 115 - 65 = 50
        assertThat(updated.getTotalBalance()).isEqualTo(50.00);
        // Verify remaining withdrawable amount is now 20.00
        assertThat(updated.getEffectiveBalance()).isEqualTo(20.00);
    }

    @Test
    @DisplayName("Withdrawal exceeding Effective Balance must be rejected without changing coins or balance")
    void testWithdrawFundsExceedingEffectiveBalanceRejected() throws SQLException {
        Wallet senderWallet = walletService.getWallet(sender.getId());
        senderWallet.setAvailableBalance(20.00);
        senderWallet.setLockedSavings(30.00);
        senderWallet.setTargetBalance(200.00);
        senderWallet.setReleaseCoins(20.00);
        walletDao.update(senderWallet);

        // Attempt to withdraw 50.00 (exceeds effective balance of 20.00)
        boolean withdrawn = walletService.withdrawFunds(sender.getId(), 50.00, true);
        assertThat(withdrawn).isFalse();

        Wallet unchanged = walletService.getWallet(sender.getId());
        assertThat(unchanged.getAvailableBalance()).isEqualTo(20.00);
        assertThat(unchanged.getLockedSavings()).isEqualTo(30.00);
        assertThat(unchanged.getReleaseCoins()).isEqualTo(20.00);
        assertThat(unchanged.getTotalBalance()).isEqualTo(50.00);
    }

    @Test
    @DisplayName("Withdrawal when target is completed unlocks all funds without spending coins")
    void testWithdrawWhenTargetCompletedDoesNotSpendCoins() throws SQLException {
        Wallet senderWallet = walletService.getWallet(sender.getId());
        senderWallet.setAvailableBalance(200.00);
        senderWallet.setLockedSavings(50.00);
        senderWallet.setTargetBalance(200.00);
        senderWallet.setReleaseCoins(40.00);
        walletDao.update(senderWallet);

        // Target achieved: full balance 250 is unlocked
        boolean withdrawn = walletService.withdrawFunds(sender.getId(), 50.00, true);
        assertThat(withdrawn).isTrue();

        Wallet updated = walletService.getWallet(sender.getId());
        // Coins must remain untouched because goal was completed
        assertThat(updated.getReleaseCoins()).isEqualTo(40.00);
        // Total balance reduced by 50
        assertThat(updated.getTotalBalance()).isEqualTo(200.00);
    }

    @Test
    @DisplayName("Withdrawal is strictly capped by Release Coins when target is incomplete (cannot withdraw 95 with 85 coins)")
    void testWithdrawalStrictlyCappedByReleaseCoinsWhenTargetIncomplete() throws SQLException {
        // User with ₹125 total balance (95 available, 30 locked), target 200, and 85 Release Coins
        Wallet senderWallet = walletService.getWallet(sender.getId());
        senderWallet.setAvailableBalance(95.00);
        senderWallet.setLockedSavings(30.00);
        senderWallet.setTargetBalance(200.00);
        senderWallet.setReleaseCoins(85.00);
        walletDao.update(senderWallet);

        // Effective withdrawable balance MUST be min(125, 85) = 85.00, NOT 95.00
        Wallet loaded = walletService.getWallet(sender.getId());
        assertThat(loaded.getEffectiveBalance()).isEqualTo(85.00);
        assertThat(loaded.getEffectiveLockedSavings()).isEqualTo(40.00);

        // Attempting to withdraw 95.00 MUST be rejected because coins = 85
        boolean withdraw95 = walletService.withdrawFunds(sender.getId(), 95.00, true);
        assertThat(withdraw95).isFalse();

        // Attempting to withdraw 85.00 MUST succeed and consume all 85 coins
        boolean withdraw85 = walletService.withdrawFunds(sender.getId(), 85.00, true);
        assertThat(withdraw85).isTrue();

        Wallet afterWithdraw = walletService.getWallet(sender.getId());
        assertThat(afterWithdraw.getReleaseCoins()).isEqualTo(0.00);
        assertThat(afterWithdraw.getTotalBalance()).isEqualTo(40.00);
        assertThat(afterWithdraw.getEffectiveBalance()).isEqualTo(0.00);
        assertThat(afterWithdraw.getEffectiveLockedSavings()).isEqualTo(40.00);
    }

    @Test
    @DisplayName("Target cannot be changed without Guardian permission")
    void testTargetCannotBeChangedWithoutGuardianPermission() {
        Wallet initial = walletService.getWallet(sender.getId());
        double oldTarget = initial.getTargetBalance();

        // 1. Without guardian approval flag, direct attempt must fail
        boolean changedWithoutApproval = walletService.updateSavingsTarget(sender.getId(), 350.00, false);
        assertThat(changedWithoutApproval).isFalse();

        // 2. Overload without active approval record in DB must fail
        boolean changedWithoutDbApproval = walletService.updateSavingsTarget(sender.getId(), 350.00);
        assertThat(changedWithoutDbApproval).isFalse();

        // Verify target remained unchanged
        Wallet unchanged = walletService.getWallet(sender.getId());
        assertThat(unchanged.getTargetBalance()).isEqualTo(oldTarget);
    }

    @Test
    @DisplayName("Target successfully updates when Guardian permission is granted")
    void testTargetUpdatesWithGuardianPermission() {
        // 1. Grant guardian permission
        walletService.setGuardianTargetPermission(sender.getId(), true, "Mom");
        assertThat(walletService.isGuardianTargetPermissionGranted(sender.getId())).isTrue();

        // 2. Target change now succeeds
        boolean changed = walletService.updateSavingsTarget(sender.getId(), 350.00, true);
        assertThat(changed).isTrue();

        Wallet updated = walletService.getWallet(sender.getId());
        assertThat(updated.getTargetBalance()).isEqualTo(350.00);
    }
}
