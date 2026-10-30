package com.example.myfirstapp.service;

import com.example.myfirstapp.dao.UserDao;
import com.example.myfirstapp.dao.WalletDao;
import com.example.myfirstapp.model.User;
import com.example.myfirstapp.model.Wallet;

import java.sql.SQLException;
import java.util.Optional;

public class AuthService {
    private static AuthService instance;
    private final UserDao userDao;
    private final WalletDao walletDao;
    private User currentUser;

    public AuthService() {
        this.userDao = new UserDao();
        this.walletDao = new WalletDao();
    }

    public AuthService(UserDao userDao, WalletDao walletDao) {
        this.userDao = userDao;
        this.walletDao = walletDao;
    }

    public static synchronized AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    public boolean login(String usernameOrEmail, String password) {
        if (usernameOrEmail == null || password == null || usernameOrEmail.isBlank() || password.isBlank()) {
            return false;
        }

        Optional<User> userOpt = userDao.findByPhoneOrEmailOrUsername(usernameOrEmail.trim());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Matching existing app default 'admin' / 'password' and registered users
            if (passwordMatches(password.trim(), user.getPasswordHash())) {
                this.currentUser = user;
                return true;
            }
        }
        return false;
    }

    /** Salted SHA-256 ("sha256:salt:hash"). Legacy plain-text seed passwords still verify. */
    public static String hashPassword(String password) {
        byte[] salt = new byte[8];
        new java.security.SecureRandom().nextBytes(salt);
        String saltHex = java.util.HexFormat.of().formatHex(salt);
        return "sha256:" + saltHex + ":" + sha256(saltHex + password);
    }

    static boolean passwordMatches(String password, String stored) {
        if (stored == null) return false;
        if (stored.startsWith("sha256:")) {
            String[] parts = stored.split(":");
            return parts.length == 3 && sha256(parts[1] + password).equals(parts[2]);
        }
        return password.equals(stored);
    }

    private static String sha256(String s) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Returns null when registration is valid, otherwise a user-facing error message. */
    public String validateRegistration(String username, String password, String fullName, String email, String phone) {
        if (username == null || !username.trim().matches("[A-Za-z0-9_.]{3,30}"))
            return "Username must be 3-30 letters, digits, _ or .";
        if (password == null || password.trim().length() < 6) return "Password must be at least 6 characters.";
        if (fullName == null || fullName.isBlank()) return "Full name is required.";
        if (email != null && !email.isBlank() && !email.trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))
            return "Enter a valid email address.";
        if (phone != null && !phone.isBlank() && !phone.trim().matches("\\+?[0-9 \\-]{7,15}"))
            return "Enter a valid phone number.";
        if (userDao.findByUsername(username.trim()).isPresent()) return "Username already taken.";
        if (email != null && !email.isBlank() && userDao.findByPhoneOrEmailOrUsername(email.trim()).isPresent())
            return "Email already registered.";
        if (phone != null && !phone.isBlank() && userDao.findByPhoneOrEmailOrUsername(phone.trim()).isPresent())
            return "Phone already registered.";
        return null;
    }

    public boolean register(String username, String password, String fullName, String email, String phone, String educationLevel, String subjects) {
        if (validateRegistration(username, password, fullName, email, phone) != null) {
            return false;
        }

        String referralCode = "SAVE-" + username.toUpperCase().replaceAll("[^A-Z0-9]", "") + "-" + (100 + (int)(Math.random() * 900));
        User newUser = new User(0, username.trim(), hashPassword(password.trim()), fullName.trim(), email == null ? "" : email.trim(), phone == null ? "" : phone.trim(),
                "STUDENT", educationLevel, subjects, referralCode, null);
        // Fresh accounts start with a small demo balance so app-to-app payments can be tested.

        try {
            User saved = userDao.create(newUser);
            // Initialize wallet with default $100.50 available balance, $50 locked, $200 target, 50 coins
            Wallet wallet = new Wallet(saved.getId(), 100.50, 50.00, 200.00, 50.00);
            walletDao.create(wallet);
            this.currentUser = saved;
            return true;
        } catch (SQLException e) {
            System.err.println("Registration failed: " + e.getMessage());
            return false;
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public void logout() {
        this.currentUser = null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }
}
