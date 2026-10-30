package com.example.myfirstapp;

import com.example.myfirstapp.dao.ApprovalDao;
import com.example.myfirstapp.dao.DatabaseManager;
import com.example.myfirstapp.dao.GuardianDao;
import com.example.myfirstapp.dao.UserDao;
import com.example.myfirstapp.dao.WalletDao;
import com.example.myfirstapp.model.User;
import com.example.myfirstapp.model.Wallet;
import com.example.myfirstapp.service.EarlyReleaseService;
import com.example.myfirstapp.service.MedicalVerificationService;
import com.example.myfirstapp.service.ReleaseCoinService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EarlyReleaseServiceTest {
    private DatabaseManager dbManager;
    private EarlyReleaseService earlyReleaseService;
    private WalletDao walletDao;
    private UserDao userDao;
    private User testUser;

    @BeforeEach
    void setUp() {
        String testDbUrl = "jdbc:sqlite:test_early_" + UUID.randomUUID() + ".db";
        DatabaseManager.resetWithUrl(testDbUrl);
        dbManager = DatabaseManager.getInstance();

        ApprovalDao approvalDao = new ApprovalDao(dbManager);
        GuardianDao guardianDao = new GuardianDao(dbManager);
        walletDao = new WalletDao(dbManager);
        userDao = new UserDao(dbManager);
        ReleaseCoinService coinService = new ReleaseCoinService();
        MedicalVerificationService medicalService = new MedicalVerificationService();

        earlyReleaseService = new EarlyReleaseService(approvalDao, guardianDao, walletDao, coinService, medicalService);
        testUser = userDao.findByUsername("admin").orElseThrow();

        // Ensure baseline: available 100.50, target 200.00 (target NOT yet reached)
        Wallet wallet = walletDao.findByUserId(testUser.getId()).orElseThrow();
        wallet.setAvailableBalance(100.50);
        wallet.setTargetBalance(200.00);
        try {
            walletDao.update(wallet);
        } catch (Exception ignored) {}
    }

    @Test
    @DisplayName("Early withdrawal is locked when target is not reached and no approvals granted")
    void testEarlyWithdrawalLockedByDefault() {
        boolean authorized = earlyReleaseService.isEarlyWithdrawalAuthorized(testUser.getId(), 50.0);
        assertThat(authorized).isFalse();
    }

    @Test
    @DisplayName("Option 1: Parent approval successfully unlocks early release")
    void testParentApprovalWorkflow() {
        boolean approved = earlyReleaseService.processParentApproval(testUser.getId(), "Mom", "Parent", true);
        assertThat(approved).isTrue();

        assertThat(earlyReleaseService.isParentApproved(testUser.getId())).isTrue();
        assertThat(earlyReleaseService.isEarlyWithdrawalAuthorized(testUser.getId(), 50.0)).isTrue();
    }

    @Test
    @DisplayName("Option 2: Close friend approval successfully unlocks early release")
    void testCloseFriendApprovalWorkflow() {
        boolean approved = earlyReleaseService.processFriendApproval(testUser.getId(), "David Miller", true);
        assertThat(approved).isTrue();

        assertThat(earlyReleaseService.isFriendApproved(testUser.getId())).isTrue();
        assertThat(earlyReleaseService.isEarlyWithdrawalAuthorized(testUser.getId(), 50.0)).isTrue();
    }

    @Test
    @DisplayName("Option 3: Medical emergency AI verification succeeds on genuine urgent clinical data")
    void testMedicalEmergencyAIVerificationSuccess() {
        MedicalVerificationService.VerificationResult result = earlyReleaseService.processMedicalEmergencySubmission(
                testUser.getId(),
                "City General Hospital",
                "MCI-98421",
                "Acute appendicitis requiring emergency surgical operation and ICU hospitalization",
                "Patient admitted to emergency department."
        );

        assertThat(result.isApproved()).isTrue();
        assertThat(result.getConfidenceScore()).isGreaterThanOrEqualTo(70);
        assertThat(earlyReleaseService.isMedicalApproved(testUser.getId())).isTrue();
        assertThat(earlyReleaseService.isEarlyWithdrawalAuthorized(testUser.getId(), 50.0)).isTrue();
    }

    @Test
    @DisplayName("Option 3: Medical emergency AI verification rejects routine or incomplete reports")
    void testMedicalEmergencyAIVerificationFailure() {
        MedicalVerificationService.VerificationResult result = earlyReleaseService.processMedicalEmergencySubmission(
                testUser.getId(),
                "General Wellness Clinic",
                "REG-1029",
                "Routine dental hygiene checkup and seasonal mild cold",
                "Patient prescribed multivitamins."
        );

        assertThat(result.isApproved()).isFalse();
        assertThat(result.getFailureReason()).contains("routine or non-emergency condition");
        assertThat(earlyReleaseService.isMedicalApproved(testUser.getId())).isFalse();
    }

    @Test
    @DisplayName("Direct withdrawal is unconditionally unlocked when target balance is achieved")
    void testDirectWithdrawalUnlockedWhenTargetAchieved() {
        // Set available balance to 250.00 (exceeds target of 200.00)
        Wallet wallet = walletDao.findByUserId(testUser.getId()).orElseThrow();
        wallet.setAvailableBalance(250.00);
        try {
            walletDao.update(wallet);
        } catch (Exception ignored) {}

        boolean authorized = earlyReleaseService.isEarlyWithdrawalAuthorized(testUser.getId(), 50.0);
        assertThat(authorized).isTrue();
    }

    @Test
    @DisplayName("Wards appear for guardian and guardian can grant or revoke early withdrawal permissions")
    void testWardPermissionsWorkflow() {
        GuardianDao guardianDao = new GuardianDao(dbManager);
        User sarah = userDao.findByUsername("sarah").orElseThrow();

        // Check that Sarah appears as a ward for Alex
        var wards = guardianDao.findWardsForGuardian(testUser.getId(), testUser.getFullName(), testUser.getUsername());
        assertThat(wards).isNotEmpty();
        assertThat(wards.stream().anyMatch(w -> w.getWardUserId() == sarah.getId())).isTrue();

        // Alex grants early withdrawal permission for Sarah
        earlyReleaseService.setWardWithdrawalPermission(sarah.getId(), "Parent", true, testUser.getFullName());
        assertThat(earlyReleaseService.isWardWithdrawalApproved(sarah.getId(), "Parent")).isTrue();
        assertThat(earlyReleaseService.isEarlyWithdrawalAuthorized(sarah.getId(), 20.0)).isTrue();

        // Alex revokes permission
        earlyReleaseService.setWardWithdrawalPermission(sarah.getId(), "Parent", false, testUser.getFullName());
        assertThat(earlyReleaseService.isWardWithdrawalApproved(sarah.getId(), "Parent")).isFalse();
    }

    @Test
    @DisplayName("Guardian can grant and revoke target modification permission for ward")
    void testWardTargetPermissionWorkflow() {
        com.example.myfirstapp.service.WalletService walletService = new com.example.myfirstapp.service.WalletService();
        User sarah = userDao.findByUsername("sarah").orElseThrow();

        // Initially target permission is not granted
        walletService.setGuardianTargetPermission(sarah.getId(), false, testUser.getFullName());
        assertThat(walletService.isGuardianTargetPermissionGranted(sarah.getId())).isFalse();

        // Target modification fails without permission
        boolean updateFailed = walletService.updateSavingsTarget(sarah.getId(), 400.0);
        assertThat(updateFailed).isFalse();

        // Guardian grants target permission
        walletService.setGuardianTargetPermission(sarah.getId(), true, testUser.getFullName());
        assertThat(walletService.isGuardianTargetPermissionGranted(sarah.getId())).isTrue();

        // Target modification now succeeds
        boolean updateSuccess = walletService.updateSavingsTarget(sarah.getId(), 400.0);
        assertThat(updateSuccess).isTrue();
        assertThat(walletService.getWallet(sarah.getId()).getTargetBalance()).isEqualTo(400.0);

        // Revoking permission blocks subsequent changes
        walletService.setGuardianTargetPermission(sarah.getId(), false, testUser.getFullName());
        assertThat(walletService.updateSavingsTarget(sarah.getId(), 500.0)).isFalse();
    }
}
