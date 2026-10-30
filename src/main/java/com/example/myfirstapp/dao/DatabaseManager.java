package com.example.myfirstapp.dao;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Manages JDBC connections and schema initialization.
 * Supports SQLite (default, zero installation required) and MySQL.
 */
public class DatabaseManager {
    private static DatabaseManager instance;
    private String jdbcUrl;
    private String username;
    private String password;
    private String driverClass;

    private DatabaseManager() {
        loadConfiguration();
        initializeDatabase();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Allows test fixtures to supply an in-memory or custom JDBC URL.
     */
    public static synchronized void resetWithUrl(String customUrl) {
        if (instance != null) {
            instance = null;
        }
        System.setProperty("smartsave.db.url", customUrl);
        getInstance();
    }

    private void loadConfiguration() {
        Properties props = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/com/example/myfirstapp/db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception ignored) {
        }

        String sysUrl = System.getProperty("smartsave.db.url");
        if (sysUrl == null || sysUrl.isBlank()) sysUrl = System.getenv("SMARTSAVE_DB_URL");
        this.jdbcUrl = (sysUrl != null && !sysUrl.isBlank()) ? sysUrl : props.getProperty("db.url", "jdbc:sqlite:smartsave.db");
        this.username = props.getProperty("db.user", "");
        this.password = props.getProperty("db.password", "");
        this.driverClass = props.getProperty("db.driver", "org.sqlite.JDBC");

        String envUser = System.getenv("SMARTSAVE_DB_USER");
        String envPass = System.getenv("SMARTSAVE_DB_PASSWORD");
        if (envUser != null && !envUser.isBlank()) this.username = envUser;
        if (envPass != null) this.password = envPass;
        if (sysUrl != null && sysUrl.startsWith("jdbc:mysql:")) this.driverClass = "com.mysql.cj.jdbc.Driver";

        if (sysUrl != null && !sysUrl.isBlank() && sysUrl.startsWith("jdbc:sqlite:")) {
            this.username = "";
            this.password = "";
            this.driverClass = "org.sqlite.JDBC";
        }

        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            System.err.println("JDBC Driver class not found: " + driverClass);
        }

        // MySQL is the primary store; fall back to embedded SQLite only if the server is unreachable.
        if (isMySql() && !canConnect()) {
            System.err.println("[SmartSave] MySQL server unreachable at " + jdbcUrl + " - falling back to local SQLite (smartsave.db).");
            this.jdbcUrl = "jdbc:sqlite:smartsave.db";
            this.username = "";
            this.password = "";
            this.driverClass = "org.sqlite.JDBC";
            try {
                Class.forName(driverClass);
            } catch (ClassNotFoundException ignored) {
            }
        }
    }

    public boolean isMySql() {
        return jdbcUrl != null && jdbcUrl.startsWith("jdbc:mysql:");
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    private boolean canConnect() {
        try (Connection c = getConnection()) {
            return c != null;
        } catch (SQLException e) {
            System.err.println("[SmartSave] MySQL connection failed: " + e.getMessage());
            return false;
        }
    }

    /** Translates SQLite-flavoured SQL into the dialect of the active database. */
    public String adapt(String sql) {
        if (!isMySql()) return sql;
        return sql.replace("INSERT OR IGNORE", "INSERT IGNORE")
                  .replace("INSERT OR REPLACE INTO", "REPLACE INTO")
                  .replace("INTEGER PRIMARY KEY AUTOINCREMENT", "INT PRIMARY KEY AUTO_INCREMENT")
                  .replace("INTEGER", "INT");
    }

    public Connection getConnection() throws SQLException {
        if (username != null && !username.isBlank()) {
            return DriverManager.getConnection(jdbcUrl, username, password);
        }
        return DriverManager.getConnection(jdbcUrl);
    }

    private void initializeDatabase() {
        try (Connection conn = getConnection()) {
            runSchemaScript(conn);
            seedInitialData(conn);
            ensureSampleWardRelationships(conn);
        } catch (SQLException e) {
            System.err.println("Error initializing database schema: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void runSchemaScript(Connection conn) {
        try (InputStream in = getClass().getResourceAsStream("/com/example/myfirstapp/db/schema.sql")) {
            if (in == null) {
                System.err.println("schema.sql resource not found!");
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                 Statement stmt = conn.createStatement()) {

                StringBuilder sqlBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("--")) {
                        continue;
                    }
                    sqlBuilder.append(line).append(" ");
                    if (line.endsWith(";")) {
                        String statement = sqlBuilder.toString().replace(";", "").trim();
                        if (!statement.isEmpty()) {
                            stmt.execute(adapt(statement));
                        }
                        sqlBuilder.setLength(0);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed running schema script: " + e.getMessage());
        }
    }

    private void seedInitialData(Connection conn) {
        // Ensure default 'admin' user exists matching original app credentials (admin / password)
        try (PreparedStatement check = conn.prepareStatement("SELECT id FROM users WHERE username = ?")) {
            check.setString(1, "admin");
            try (ResultSet rs = check.executeQuery()) {
                if (!rs.next()) {
                    // Insert admin
                    try (PreparedStatement insert = conn.prepareStatement(
                            "INSERT INTO users (username, password_hash, full_name, email, phone, role, education_level, subjects, referral_code) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                        insert.setString(1, "admin");
                        insert.setString(2, "password"); // In production, use BCrypt or Argon2
                        insert.setString(3, "Alex Hunter");
                        insert.setString(4, "alex.hunter@student.edu");
                        insert.setString(5, "+1 555-0192");
                        insert.setString(6, "STUDENT");
                        insert.setString(7, "B.Tech Computer Science (Semester 5)");
                        insert.setString(8, "Data Structures, Financial Economics, AI");
                        insert.setString(9, "SAVE-SMART-789");
                        insert.executeUpdate();

                        try (ResultSet generatedKeys = insert.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                int adminId = generatedKeys.getInt(1);
                                // Initialize wallet: Available: 100.50, Locked: 50.00, Target: 200.00, Coins: 50.00
                                try (PreparedStatement insertWallet = conn.prepareStatement(
                                        "INSERT INTO wallets (user_id, available_balance, locked_savings, target_balance, release_coins) " +
                                        "VALUES (?, 100.50, 50.00, 200.00, 50.00)")) {
                                    insertWallet.setInt(1, adminId);
                                    insertWallet.executeUpdate();
                                }

                                // Seed default guardians from original app: "Mom (Parent), Alex (Close Friend)"
                                try (PreparedStatement insertG1 = conn.prepareStatement(
                                        "INSERT INTO guardians (user_id, name, relationship, status) VALUES (?, 'Mom', 'Parent', 'ACTIVE')")) {
                                    insertG1.setInt(1, adminId);
                                    insertG1.executeUpdate();
                                }
                                try (PreparedStatement insertG2 = conn.prepareStatement(
                                        "INSERT INTO guardians (user_id, name, relationship, status) VALUES (?, 'David Miller', 'Close Friend', 'ACTIVE')")) {
                                    insertG2.setInt(1, adminId);
                                    insertG2.executeUpdate();
                                }
                            }
                        }
                    }

                    // Seed second registered student 'sarah' for peer-to-peer virtual transfers
                    try (PreparedStatement insertSarah = conn.prepareStatement(
                            "INSERT INTO users (username, password_hash, full_name, email, phone, role, education_level, subjects, referral_code) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                        insertSarah.setString(1, "sarah");
                        insertSarah.setString(2, "password");
                        insertSarah.setString(3, "Sarah Jenkins");
                        insertSarah.setString(4, "sarah.j@student.edu");
                        insertSarah.setString(5, "+1 555-0193");
                        insertSarah.setString(6, "STUDENT");
                        insertSarah.setString(7, "B.Com Finance (Year 2)");
                        insertSarah.setString(8, "Accounting, Microeconomics");
                        insertSarah.setString(9, "SARAH-SAVE-101");
                        insertSarah.executeUpdate();

                        try (ResultSet gk = insertSarah.getGeneratedKeys()) {
                            if (gk.next()) {
                                int sarahId = gk.getInt(1);
                                try (PreparedStatement insertWallet = conn.prepareStatement(
                                        "INSERT INTO wallets (user_id, available_balance, locked_savings, target_balance, release_coins) " +
                                        "VALUES (?, 85.00, 100.00, 300.00, 25.00)")) {
                                    insertWallet.setInt(1, sarahId);
                                    insertWallet.executeUpdate();
                                }
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Seed initial data error: " + e.getMessage());
        }
    }

    private void ensureSampleWardRelationships(Connection conn) {
        try {
            int sarahId = -1;
            int adminId = -1;
            try (PreparedStatement findUsers = conn.prepareStatement("SELECT id, username FROM users WHERE username IN ('sarah', 'admin')")) {
                try (ResultSet rs = findUsers.executeQuery()) {
                    while (rs.next()) {
                        if ("sarah".equalsIgnoreCase(rs.getString("username"))) {
                            sarahId = rs.getInt("id");
                        } else if ("admin".equalsIgnoreCase(rs.getString("username"))) {
                            adminId = rs.getInt("id");
                        }
                    }
                }
            }

            if (sarahId != -1 && adminId != -1) {
                String checkSql = "SELECT id FROM guardians WHERE user_id = ? AND (guardian_user_id = ? OR LOWER(name) = 'alex hunter')";
                try (PreparedStatement check = conn.prepareStatement(checkSql)) {
                    check.setInt(1, sarahId);
                    check.setInt(2, adminId);
                    try (ResultSet rs = check.executeQuery()) {
                        if (!rs.next()) {
                            try (PreparedStatement insert = conn.prepareStatement(
                                    "INSERT INTO guardians (user_id, guardian_user_id, name, relationship, status) VALUES (?, ?, 'Alex Hunter', 'Parent', 'ACTIVE')")) {
                                insert.setInt(1, sarahId);
                                insert.setInt(2, adminId);
                                insert.executeUpdate();
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Notice: Could not check/seed sample ward relationship: " + e.getMessage());
        }
    }
}
