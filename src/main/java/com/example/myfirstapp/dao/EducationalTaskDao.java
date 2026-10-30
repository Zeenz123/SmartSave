package com.example.myfirstapp.dao;

import com.example.myfirstapp.model.EducationalTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EducationalTaskDao {
    private final DatabaseManager dbManager;

    public EducationalTaskDao() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public EducationalTaskDao(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public boolean isTaskCompleted(int userId, String taskKey) {
        String sql = "SELECT id FROM educational_tasks WHERE user_id = ? AND task_key = ? AND completed = 1";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(dbManager.adapt(sql))) {
            stmt.setInt(1, userId);
            stmt.setString(2, taskKey);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error checking task completion: " + e.getMessage());
        }
        return false;
    }

    public boolean recordTaskCompletion(int userId, String taskKey, String title, String category,
                                        String resourceUrl, int minSeconds, int timeSpentSeconds, double coinsRewarded) {
        if (isTaskCompleted(userId, taskKey)) {
            return false; // Prevent duplicate rewards
        }

        String sql = "INSERT OR REPLACE INTO educational_tasks (user_id, task_key, title, category, resource_url, min_seconds, time_spent_seconds, completed, coins_rewarded, completed_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(dbManager.adapt(sql))) {
            stmt.setInt(1, userId);
            stmt.setString(2, taskKey);
            stmt.setString(3, title);
            stmt.setString(4, category);
            stmt.setString(5, resourceUrl);
            stmt.setInt(6, minSeconds);
            stmt.setInt(7, timeSpentSeconds);
            stmt.setDouble(8, coinsRewarded);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error recording task completion: " + e.getMessage());
            return false;
        }
    }

    public List<EducationalTask> findCompletedTasks(int userId) {
        List<EducationalTask> list = new ArrayList<>();
        String sql = "SELECT * FROM educational_tasks WHERE user_id = ? AND completed = 1 ORDER BY id DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(dbManager.adapt(sql))) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new EducationalTask(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getString("task_key"),
                            rs.getString("title"),
                            rs.getString("category"),
                            rs.getString("resource_url"),
                            rs.getInt("min_seconds"),
                            rs.getInt("time_spent_seconds"),
                            rs.getInt("completed") == 1,
                            rs.getDouble("coins_rewarded"),
                            LocalDateTime.now()
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error listing completed tasks: " + e.getMessage());
        }
        return list;
    }
}
