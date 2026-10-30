package com.example.myfirstapp.dao;

import com.example.myfirstapp.model.Transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransactionDao {
    private final DatabaseManager dbManager;

    public TransactionDao() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public TransactionDao(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public Transaction record(Integer senderId, Integer recipientId, double amount,
                              Transaction.Type type, Transaction.Status status, String description) {
        String txId = "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String sql = "INSERT INTO transactions (transaction_id, sender_id, recipient_id, amount, type, status, description) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, txId);
            if (senderId != null) stmt.setInt(2, senderId); else stmt.setNull(2, java.sql.Types.INTEGER);
            if (recipientId != null) stmt.setInt(3, recipientId); else stmt.setNull(3, java.sql.Types.INTEGER);
            stmt.setDouble(4, amount);
            stmt.setString(5, type.name());
            stmt.setString(6, status.name());
            stmt.setString(7, description);
            stmt.executeUpdate();

            return new Transaction(0, txId, senderId, recipientId, amount, type, status, description, LocalDateTime.now());
        } catch (SQLException e) {
            System.err.println("Error recording transaction: " + e.getMessage());
            return null;
        }
    }

    public List<Transaction> findByUserId(int userId) {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE sender_id = ? OR recipient_id = ? ORDER BY id DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching transactions: " + e.getMessage());
        }
        return list;
    }

    private Transaction mapResultSet(ResultSet rs) throws SQLException {
        Integer senderId = rs.getObject("sender_id") != null ? rs.getInt("sender_id") : null;
        Integer recipientId = rs.getObject("recipient_id") != null ? rs.getInt("recipient_id") : null;
        return new Transaction(
                rs.getInt("id"),
                rs.getString("transaction_id"),
                senderId,
                recipientId,
                rs.getDouble("amount"),
                Transaction.Type.valueOf(rs.getString("type")),
                Transaction.Status.valueOf(rs.getString("status")),
                rs.getString("description"),
                parseTimestamp(rs.getString("created_at"))
        );
    }

    private static LocalDateTime parseTimestamp(String raw) {
        if (raw == null) return LocalDateTime.now();
        try {
            return LocalDateTime.parse(raw.trim().replace(' ', 'T'));
        } catch (RuntimeException e) {
            return LocalDateTime.now();
        }
    }
}
