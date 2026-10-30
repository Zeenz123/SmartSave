package com.example.myfirstapp.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AdEventDao {
    private final DatabaseManager dbManager;

    public AdEventDao() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public AdEventDao(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public boolean isAdWatched(int userId, String adToken) {
        String sql = "SELECT id FROM ad_events WHERE user_id = ? AND ad_token = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, adToken);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error checking ad event: " + e.getMessage());
        }
        return false;
    }

    public boolean recordAdWatch(int userId, String adToken, double coinsAwarded) {
        if (isAdWatched(userId, adToken)) {
            return false; // Prevent duplicate rewards for the same ad event
        }

        String sql = "INSERT INTO ad_events (user_id, ad_token, coins_awarded) VALUES (?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, adToken);
            stmt.setDouble(3, coinsAwarded);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error recording ad watch: " + e.getMessage());
            return false;
        }
    }
}
