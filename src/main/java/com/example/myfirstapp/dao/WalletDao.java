package com.example.myfirstapp.dao;

import com.example.myfirstapp.model.Transaction;
import com.example.myfirstapp.model.Wallet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class WalletDao {
    private final DatabaseManager dbManager;

    public WalletDao() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public WalletDao(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public Optional<Wallet> findByUserId(int userId) {
        String sql = "SELECT * FROM wallets WHERE user_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Wallet(
                            rs.getInt("user_id"),
                            rs.getDouble("available_balance"),
                            rs.getDouble("locked_savings"),
                            rs.getDouble("target_balance"),
                            rs.getDouble("release_coins")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding wallet: " + e.getMessage());
        }
        return Optional.empty();
    }

    public void create(Wallet wallet) throws SQLException {
        String sql = "INSERT INTO wallets (user_id, available_balance, locked_savings, target_balance, release_coins) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, wallet.getUserId());
            stmt.setDouble(2, wallet.getAvailableBalance());
            stmt.setDouble(3, wallet.getLockedSavings());
            stmt.setDouble(4, wallet.getTargetBalance());
            stmt.setDouble(5, wallet.getReleaseCoins());
            stmt.executeUpdate();
        }
    }

    public void update(Wallet wallet) throws SQLException {
        String sql = "UPDATE wallets SET available_balance = ?, locked_savings = ?, target_balance = ?, release_coins = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, wallet.getAvailableBalance());
            stmt.setDouble(2, wallet.getLockedSavings());
            stmt.setDouble(3, wallet.getTargetBalance());
            stmt.setDouble(4, wallet.getReleaseCoins());
            stmt.setInt(5, wallet.getUserId());
            stmt.executeUpdate();
        }
    }

    /**
     * Executes an atomic ACID transaction for P2P virtual money transfer.
     */
    public synchronized boolean transferFunds(int senderId, int recipientId, double amount, String description) throws SQLException {
        if (amount <= 0 || senderId == recipientId) {
            return false;
        }

        String selectSender = "SELECT available_balance FROM wallets WHERE user_id = ?";
        String deductSql = "UPDATE wallets SET available_balance = available_balance - ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";
        String addSql = "UPDATE wallets SET available_balance = available_balance + ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";
        String txSql = "INSERT INTO transactions (transaction_id, sender_id, recipient_id, amount, type, status, description) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Verify sender balance
                double senderBal = 0;
                try (PreparedStatement stmt = conn.prepareStatement(selectSender)) {
                    stmt.setInt(1, senderId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                        senderBal = rs.getDouble("available_balance");
                    }
                }

                if (senderBal < amount) {
                    conn.rollback();
                    return false;
                }

                // 2. Deduct from sender
                try (PreparedStatement stmt = conn.prepareStatement(deductSql)) {
                    stmt.setDouble(1, amount);
                    stmt.setInt(2, senderId);
                    int updated = stmt.executeUpdate();
                    if (updated == 0) {
                        conn.rollback();
                        return false;
                    }
                }

                // 3. Add to recipient
                try (PreparedStatement stmt = conn.prepareStatement(addSql)) {
                    stmt.setDouble(1, amount);
                    stmt.setInt(2, recipientId);
                    int updated = stmt.executeUpdate();
                    if (updated == 0) {
                        conn.rollback();
                        return false;
                    }
                }

                // 4. Record persistent transaction
                String txId = "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                try (PreparedStatement stmt = conn.prepareStatement(txSql)) {
                    stmt.setString(1, txId);
                    stmt.setInt(2, senderId);
                    stmt.setInt(3, recipientId);
                    stmt.setDouble(4, amount);
                    stmt.setString(5, Transaction.Type.TRANSFER.name());
                    stmt.setString(6, Transaction.Status.SUCCESS.name());
                    stmt.setString(7, description != null ? description : "P2P Virtual Transfer");
                    stmt.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
