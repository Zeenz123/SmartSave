package com.example.myfirstapp.dao;

import com.example.myfirstapp.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ConnectionDao {
    private final DatabaseManager dbManager;

    public ConnectionDao() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public ConnectionDao(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public boolean addConnection(int userId, int targetUserId, String relationshipType) {
        if (userId == targetUserId) return false;
        String sql = "INSERT OR IGNORE INTO connections (user_id, connected_user_id, relationship_type, status) VALUES (?, ?, ?, 'CONNECTED')";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(dbManager.adapt(sql))) {
            stmt.setInt(1, userId);
            stmt.setInt(2, targetUserId);
            stmt.setString(3, relationshipType);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error adding connection: " + e.getMessage());
            return false;
        }
    }

    public List<Integer> getConnectedUserIds(int userId) {
        List<Integer> list = new ArrayList<>();
        String sql = "SELECT connected_user_id FROM connections WHERE user_id = ? AND status = 'CONNECTED'";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(dbManager.adapt(sql))) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getInt("connected_user_id"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching connections: " + e.getMessage());
        }
        return list;
    }
}
