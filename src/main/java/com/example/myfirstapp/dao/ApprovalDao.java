package com.example.myfirstapp.dao;

import com.example.myfirstapp.model.ApprovalRequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ApprovalDao {
    private final DatabaseManager dbManager;

    public ApprovalDao() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public ApprovalDao(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public boolean isApproved(int userId, ApprovalRequest.Type type) {
        String sql = "SELECT id FROM approvals WHERE user_id = ? AND approval_type = ? AND status = 'APPROVED'";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, type.name());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error checking approval: " + e.getMessage());
        }
        return false;
    }

    public void setApproval(int userId, ApprovalRequest.Type type, ApprovalRequest.Status status, String notes, String verificationData) {
        // Upsert or update status
        String checkSql = "SELECT id FROM approvals WHERE user_id = ? AND approval_type = ?";
        String updateSql = "UPDATE approvals SET status = ?, requester_notes = ?, verification_data = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        String insertSql = "INSERT INTO approvals (user_id, approval_type, status, requester_notes, verification_data) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection()) {
            Integer existingId = null;
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, userId);
                checkStmt.setString(2, type.name());
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        existingId = rs.getInt("id");
                    }
                }
            }

            if (existingId != null) {
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, status.name());
                    updateStmt.setString(2, notes);
                    updateStmt.setString(3, verificationData);
                    updateStmt.setInt(4, existingId);
                    updateStmt.executeUpdate();
                }
            } else {
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setInt(1, userId);
                    insertStmt.setString(2, type.name());
                    insertStmt.setString(3, status.name());
                    insertStmt.setString(4, notes);
                    insertStmt.setString(5, verificationData);
                    insertStmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error setting approval: " + e.getMessage());
        }
    }

    public List<ApprovalRequest> findByUserId(int userId) {
        List<ApprovalRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM approvals WHERE user_id = ? ORDER BY id DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new ApprovalRequest(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            ApprovalRequest.Type.valueOf(rs.getString("approval_type")),
                            ApprovalRequest.Status.valueOf(rs.getString("status")),
                            rs.getString("requester_notes"),
                            rs.getString("verification_data"),
                            LocalDateTime.now(),
                            LocalDateTime.now()
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching approvals: " + e.getMessage());
        }
        return list;
    }
}
