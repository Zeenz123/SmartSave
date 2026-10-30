package com.example.myfirstapp.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PaymentRequestDao {
    public static class Record {
        public int id;
        public int requesterId;
        public Integer targetId;
        public String targetContact;
        public double amount;
        public String status;

        public Record(int id, int requesterId, Integer targetId, String targetContact, double amount, String status) {
            this.id = id;
            this.requesterId = requesterId;
            this.targetId = targetId;
            this.targetContact = targetContact;
            this.amount = amount;
            this.status = status;
        }
    }

    private final DatabaseManager dbManager;

    public PaymentRequestDao() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public PaymentRequestDao(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public boolean createRequest(int requesterId, Integer targetId, String targetContact, double amount) {
        String sql = "INSERT INTO payment_requests (requester_id, target_id, target_contact, amount, status) VALUES (?, ?, ?, ?, 'PENDING')";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, requesterId);
            if (targetId != null) stmt.setInt(2, targetId); else stmt.setNull(2, java.sql.Types.INTEGER);
            stmt.setString(3, targetContact);
            stmt.setDouble(4, amount);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error creating payment request: " + e.getMessage());
            return false;
        }
    }

    public List<Record> findByRequesterId(int requesterId) {
        List<Record> list = new ArrayList<>();
        String sql = "SELECT * FROM payment_requests WHERE requester_id = ? ORDER BY id DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, requesterId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new Record(
                            rs.getInt("id"),
                            rs.getInt("requester_id"),
                            rs.getObject("target_id") != null ? rs.getInt("target_id") : null,
                            rs.getString("target_contact"),
                            rs.getDouble("amount"),
                            rs.getString("status")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error reading payment requests: " + e.getMessage());
        }
        return list;
    }

    private Record map(ResultSet rs) throws SQLException {
        return new Record(
                rs.getInt("id"),
                rs.getInt("requester_id"),
                rs.getObject("target_id") != null ? rs.getInt("target_id") : null,
                rs.getString("target_contact"),
                rs.getDouble("amount"),
                rs.getString("status"));
    }

    /** Requests addressed to this user that are still waiting to be paid. */
    public List<Record> findPendingForTarget(int targetId) {
        List<Record> list = new ArrayList<>();
        String sql = "SELECT * FROM payment_requests WHERE target_id = ? AND status = 'PENDING' ORDER BY id DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, targetId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error reading incoming payment requests: " + e.getMessage());
        }
        return list;
    }

    public java.util.Optional<Record> findById(int id) {
        String sql = "SELECT * FROM payment_requests WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return java.util.Optional.of(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error reading payment request: " + e.getMessage());
        }
        return java.util.Optional.empty();
    }

    /** Atomic compare-and-set so two devices can never pay the same request twice. */
    public boolean transition(int id, String from, String to) {
        String sql = "UPDATE payment_requests SET status = ? WHERE id = ? AND status = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, to);
            stmt.setInt(2, id);
            stmt.setString(3, from);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating payment request: " + e.getMessage());
            return false;
        }
    }
}
