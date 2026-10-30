package com.example.myfirstapp.dao;

import com.example.myfirstapp.model.Guardian;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GuardianDao {
    private final DatabaseManager dbManager;

    public static class WardRecord {
        private final int guardianRecordId;
        private final int wardUserId;
        private final String wardFullName;
        private final String wardUsername;
        private final String relationship;
        private final String status;
        private final double availableBalance;
        private final double lockedSavings;
        private final double targetBalance;

        public WardRecord(int guardianRecordId, int wardUserId, String wardFullName, String wardUsername,
                          String relationship, String status, double availableBalance, double lockedSavings, double targetBalance) {
            this.guardianRecordId = guardianRecordId;
            this.wardUserId = wardUserId;
            this.wardFullName = wardFullName;
            this.wardUsername = wardUsername;
            this.relationship = relationship;
            this.status = status;
            this.availableBalance = availableBalance;
            this.lockedSavings = lockedSavings;
            this.targetBalance = targetBalance;
        }

        public int getGuardianRecordId() { return guardianRecordId; }
        public int getWardUserId() { return wardUserId; }
        public String getWardFullName() { return wardFullName; }
        public String getWardUsername() { return wardUsername; }
        public String getRelationship() { return relationship; }
        public String getStatus() { return status; }
        public double getAvailableBalance() { return availableBalance; }
        public double getLockedSavings() { return lockedSavings; }
        public double getTargetBalance() { return targetBalance; }
    }

    public GuardianDao() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public GuardianDao(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public List<Guardian> findByUserId(int userId) {
        List<Guardian> list = new ArrayList<>();
        String sql = "SELECT * FROM guardians WHERE user_id = ? AND status = 'ACTIVE' ORDER BY id ASC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new Guardian(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getObject("guardian_user_id") != null ? rs.getInt("guardian_user_id") : null,
                            rs.getString("name"),
                            rs.getString("relationship"),
                            rs.getString("status"),
                            LocalDateTime.now()
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching guardians: " + e.getMessage());
        }
        return list;
    }

    /**
     * Finds all wards / friends who have added the specified user as their Guardian or authority.
     * Matches by guardian_user_id OR by matching the guardian's full name / username.
     */
    public List<WardRecord> findWardsForGuardian(int guardianUserId, String guardianName, String guardianUsername) {
        List<WardRecord> list = new ArrayList<>();
        String sql = "SELECT g.id, g.user_id, g.relationship, g.status, " +
                     "u.full_name, u.username, " +
                     "COALESCE(w.available_balance, 0.0) AS available_balance, " +
                     "COALESCE(w.locked_savings, 0.0) AS locked_savings, " +
                     "COALESCE(w.target_balance, 0.0) AS target_balance " +
                     "FROM guardians g " +
                     "JOIN users u ON g.user_id = u.id " +
                     "LEFT JOIN wallets w ON u.id = w.user_id " +
                     "WHERE (g.guardian_user_id = ? " +
                     "   OR (LOWER(?) != '' AND LOWER(g.name) = LOWER(?)) " +
                     "   OR (LOWER(?) != '' AND LOWER(g.name) = LOWER(?))) " +
                     "  AND g.user_id != ? " +
                     "  AND g.status = 'ACTIVE' " +
                     "ORDER BY g.id ASC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, guardianUserId);
            String safeName = (guardianName != null) ? guardianName.trim() : "";
            stmt.setString(2, safeName);
            stmt.setString(3, safeName);
            String safeUsername = (guardianUsername != null) ? guardianUsername.trim() : "";
            stmt.setString(4, safeUsername);
            stmt.setString(5, safeUsername);
            stmt.setInt(6, guardianUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new WardRecord(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getString("full_name"),
                            rs.getString("username"),
                            rs.getString("relationship"),
                            rs.getString("status"),
                            rs.getDouble("available_balance"),
                            rs.getDouble("locked_savings"),
                            rs.getDouble("target_balance")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching wards for guardian: " + e.getMessage());
        }
        return list;
    }

    public void addGuardian(int userId, Integer guardianUserId, String name, String relationship) {
        String sql = "INSERT INTO guardians (user_id, guardian_user_id, name, relationship, status) VALUES (?, ?, ?, ?, 'ACTIVE')";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            if (guardianUserId != null) stmt.setInt(2, guardianUserId); else stmt.setNull(2, java.sql.Types.INTEGER);
            stmt.setString(3, name);
            stmt.setString(4, relationship);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error adding guardian: " + e.getMessage());
        }
    }

    public void removeGuardian(int guardianId) {
        String sql = "UPDATE guardians SET status = 'INACTIVE' WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, guardianId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error removing guardian: " + e.getMessage());
        }
    }
}
