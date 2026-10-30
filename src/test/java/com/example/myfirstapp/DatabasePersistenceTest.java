package com.example.myfirstapp;

import com.example.myfirstapp.dao.DatabaseManager;
import com.example.myfirstapp.dao.TransactionDao;
import com.example.myfirstapp.dao.UserDao;
import com.example.myfirstapp.dao.WalletDao;
import com.example.myfirstapp.model.Transaction;
import com.example.myfirstapp.model.User;
import com.example.myfirstapp.model.Wallet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DatabasePersistenceTest {

    @Test
    @DisplayName("Data persisted via JDBC survives closing and re-opening database connection")
    void testPersistenceAcrossDatabaseSessions() throws SQLException {
        String dbFile = "test_persist_" + UUID.randomUUID() + ".db";
        String dbUrl = "jdbc:sqlite:" + dbFile;

        // Session 1: Create user, wallet, and transaction
        DatabaseManager.resetWithUrl(dbUrl);
        DatabaseManager session1Db = DatabaseManager.getInstance();
        UserDao userDao1 = new UserDao(session1Db);
        WalletDao walletDao1 = new WalletDao(session1Db);
        TransactionDao txDao1 = new TransactionDao(session1Db);

        User newUser = new User(0, "michael", "pass123", "Michael Chen", "michael@test.edu", "+15550011",
                "STUDENT", "Undergraduate", "Algorithms", "MICHAEL-777", null);
        User created = userDao1.create(newUser);
        assertThat(created.getId()).isPositive();

        Wallet newWallet = new Wallet(created.getId(), 250.75, 100.00, 500.00, 75.00);
        walletDao1.create(newWallet);

        Transaction recordedTx = txDao1.record(null, created.getId(), 250.75,
                Transaction.Type.DEPOSIT, Transaction.Status.SUCCESS, "Initial test deposit");
        assertThat(recordedTx).isNotNull();

        // Session 2: Completely reset DatabaseManager and reconnect to the same SQLite database file
        DatabaseManager.resetWithUrl(dbUrl);
        DatabaseManager session2Db = DatabaseManager.getInstance();
        UserDao userDao2 = new UserDao(session2Db);
        WalletDao walletDao2 = new WalletDao(session2Db);
        TransactionDao txDao2 = new TransactionDao(session2Db);

        // Verify User was persisted
        Optional<User> fetchedUser = userDao2.findByUsername("michael");
        assertThat(fetchedUser).isPresent();
        assertThat(fetchedUser.get().getFullName()).isEqualTo("Michael Chen");
        assertThat(fetchedUser.get().getReferralCode()).isEqualTo("MICHAEL-777");

        // Verify Wallet was persisted with exact balances
        Optional<Wallet> fetchedWallet = walletDao2.findByUserId(fetchedUser.get().getId());
        assertThat(fetchedWallet).isPresent();
        assertThat(fetchedWallet.get().getAvailableBalance()).isEqualTo(250.75);
        assertThat(fetchedWallet.get().getLockedSavings()).isEqualTo(100.00);
        assertThat(fetchedWallet.get().getReleaseCoins()).isEqualTo(75.00);

        // Verify Transaction history was persisted
        List<Transaction> txList = txDao2.findByUserId(fetchedUser.get().getId());
        assertThat(txList).isNotEmpty();
        assertThat(txList.get(0).getAmount()).isEqualTo(250.75);
        assertThat(txList.get(0).getDescription()).isEqualTo("Initial test deposit");

        // Clean up test file
        new File(dbFile).deleteOnExit();
    }
}
