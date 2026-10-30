package com.example.myfirstapp;

import com.example.myfirstapp.dao.DatabaseManager;
import com.example.myfirstapp.dao.UserDao;
import com.example.myfirstapp.dao.WalletDao;
import com.example.myfirstapp.model.User;
import com.example.myfirstapp.service.AuthService;
import com.example.myfirstapp.service.ConnectService;
import com.example.myfirstapp.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AppToAppPaymentTest {
    private AuthService auth;
    private ConnectService connect;
    private WalletService wallets;

    @BeforeEach
    void setUp() {
        DatabaseManager.resetWithUrl("jdbc:sqlite:test_app2app_" + UUID.randomUUID() + ".db");
        auth = new AuthService(new UserDao(), new WalletDao());
        connect = new ConnectService();
        wallets = new WalletService();
    }

    @Test
    @DisplayName("Two newly registered IDs: request on one side, pay from the other, request cannot be paid twice")
    void requestAndPayBetweenTwoAccounts() {
        assertThat(auth.register("riya_k", "secret1", "Riya K", "riya@x.com", "9876543210", "UG", "CS")).isTrue();
        User riya = auth.getCurrentUser();
        assertThat(auth.register("amit_s", "secret2", "Amit S", "amit@x.com", "9123456780", "UG", "CS")).isTrue();
        User amit = auth.getCurrentUser();

        // sign in with own credentials (stored hashed, not plain)
        assertThat(riya.getPasswordHash()).startsWith("sha256:");
        assertThat(auth.login("riya_k", "secret1")).isTrue();
        assertThat(auth.login("riya_k", "wrong")).isFalse();
        assertThat(auth.login("9123456780", "secret2")).isTrue(); // sign in by phone

        double riyaBefore = wallets.getWallet(riya.getId()).getAvailableBalance();
        double amitBefore = wallets.getWallet(amit.getId()).getAvailableBalance();

        // Riya (phone 1) asks Amit (phone 2) for 40
        assertThat(connect.sendPaymentRequest(riya.getId(), "amit_s", 40)).isTrue();
        List<ConnectService.IncomingRequest> incoming = connect.getIncomingRequests(amit.getId());
        assertThat(incoming).hasSize(1);
        int reqId = incoming.get(0).record.id;

        // Amit pays it
        assertThat(connect.payRequest(reqId, amit).isSuccess()).isTrue();
        assertThat(wallets.getWallet(amit.getId()).getAvailableBalance()).isEqualTo(amitBefore - 40);
        assertThat(wallets.getWallet(riya.getId()).getAvailableBalance()).isEqualTo(riyaBefore + 40);
        assertThat(connect.getIncomingRequests(amit.getId())).isEmpty();

        // Paying again is rejected and moves no money
        assertThat(connect.payRequest(reqId, amit).isSuccess()).isFalse();
        assertThat(wallets.getWallet(riya.getId()).getAvailableBalance()).isEqualTo(riyaBefore + 40);

        // Only the addressed user may pay
        connect.sendPaymentRequest(riya.getId(), "amit_s", 5);
        int second = connect.getIncomingRequests(amit.getId()).get(0).record.id;
        assertThat(connect.payRequest(second, riya).isSuccess()).isFalse();
        assertThat(connect.declineRequest(second, amit)).isTrue();
    }

    @Test
    @DisplayName("Registration validation rejects duplicates and weak input")
    void registrationValidation() {
        assertThat(auth.register("dup_user", "secret1", "Dup", "d@x.com", "9000000001", "UG", "CS")).isTrue();
        assertThat(auth.register("dup_user", "secret1", "Dup2", "e@x.com", "9000000002", "UG", "CS")).isFalse();
        assertThat(auth.validateRegistration("ab", "secret1", "X", "", "")).contains("Username");
        assertThat(auth.validateRegistration("valid_one", "123", "X", "", "")).contains("Password");
        assertThat(auth.validateRegistration("valid_one", "secret1", "X", "bad-email", "")).contains("email");
        assertThat(auth.validateRegistration("valid_two", "secret1", "X", "d@x.com", "")).contains("Email");
        assertThat(auth.validateRegistration("valid_two", "secret1", "X", "", "9000000001")).contains("Phone");
    }
}
