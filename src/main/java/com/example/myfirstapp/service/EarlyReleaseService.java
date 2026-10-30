package com.example.myfirstapp.service;

import com.example.myfirstapp.dao.ApprovalDao;
import com.example.myfirstapp.dao.GuardianDao;
import com.example.myfirstapp.dao.WalletDao;
import com.example.myfirstapp.model.ApprovalRequest;
import com.example.myfirstapp.model.Guardian;
import com.example.myfirstapp.model.Wallet;

import java.util.List;

public class EarlyReleaseService {
    private final ApprovalDao approvalDao;
    private final GuardianDao guardianDao;
    private final WalletDao walletDao;
    private final ReleaseCoinService releaseCoinService;
    private final MedicalVerificationService medicalVerificationService;

    public EarlyReleaseService() {
        this.approvalDao = new ApprovalDao();
        this.guardianDao = new GuardianDao();
        this.walletDao = new WalletDao();
        this.releaseCoinService = new ReleaseCoinService();
        this.medicalVerificationService = new MedicalVerificationService();
    }

    public EarlyReleaseService(ApprovalDao approvalDao, GuardianDao guardianDao, WalletDao walletDao,
                               ReleaseCoinService releaseCoinService, MedicalVerificationService medicalVerificationService) {
        this.approvalDao = approvalDao;
        this.guardianDao = guardianDao;
        this.walletDao = walletDao;
        this.releaseCoinService = releaseCoinService;
        this.medicalVerificationService = medicalVerificationService;
    }

    public boolean isParentApproved(int userId) {
        return approvalDao.isApproved(userId, ApprovalRequest.Type.PARENT);
    }

    public boolean isFriendApproved(int userId) {
        return approvalDao.isApproved(userId, ApprovalRequest.Type.FRIEND);
    }

    public boolean isMedicalApproved(int userId) {
        return approvalDao.isApproved(userId, ApprovalRequest.Type.MEDICAL);
    }

    public boolean isTaskApproved(int userId) {
        return approvalDao.isApproved(userId, ApprovalRequest.Type.TASK);
    }

    public boolean isCoinApproved(int userId) {
        return approvalDao.isApproved(userId, ApprovalRequest.Type.COIN);
    }

    /**
     * Option 1: Parent or Guardian Approval.
     * Verifies that the user has a registered Parent guardian and validates the request.
     */
    public boolean processParentApproval(int userId, String guardianName, String relationship, boolean granted) {
        List<Guardian> guardians = guardianDao.findByUserId(userId);
        boolean hasParent = guardians.stream()
                .anyMatch(g -> g.getRelationship().equalsIgnoreCase("Parent"));

        if (!hasParent) {
            return false; // No registered parent found
        }

        approvalDao.setApproval(userId, ApprovalRequest.Type.PARENT,
                granted ? ApprovalRequest.Status.APPROVED : ApprovalRequest.Status.REJECTED,
                "Approval submitted by guardian: " + guardianName, "Relationship: " + relationship);
        return true;
    }

    /**
     * Option 2: Close Friend Approval.
     * Verifies that the user has a registered close friend and validates the request.
     */
    public boolean processFriendApproval(int userId, String friendName, boolean granted) {
        List<Guardian> guardians = guardianDao.findByUserId(userId);
        boolean hasFriend = guardians.stream()
                .anyMatch(g -> g.getRelationship().equalsIgnoreCase("Close Friend"));

        if (!hasFriend) {
            return false; // No registered close friend found
        }

        approvalDao.setApproval(userId, ApprovalRequest.Type.FRIEND,
                granted ? ApprovalRequest.Status.APPROVED : ApprovalRequest.Status.REJECTED,
                "Approval submitted by close friend: " + friendName, "Relationship: Close Friend");
        return true;
    }

    /**
     * Sets early emergency withdrawal permission for a ward who added this user as a guardian.
     */
    public boolean setWardWithdrawalPermission(int wardUserId, String relationship, boolean granted, String guardianName) {
        ApprovalRequest.Type type = "Parent".equalsIgnoreCase(relationship)
                ? ApprovalRequest.Type.PARENT
                : ApprovalRequest.Type.FRIEND;

        approvalDao.setApproval(wardUserId, type,
                granted ? ApprovalRequest.Status.APPROVED : ApprovalRequest.Status.REJECTED,
                "Permission " + (granted ? "granted" : "revoked") + " by guardian: " + guardianName,
                "Relationship: " + relationship);
        return true;
    }

    /**
     * Checks if a ward has active early withdrawal approval for their registered relationship.
     */
    public boolean isWardWithdrawalApproved(int wardUserId, String relationship) {
        ApprovalRequest.Type type = "Parent".equalsIgnoreCase(relationship)
                ? ApprovalRequest.Type.PARENT
                : ApprovalRequest.Type.FRIEND;
        return approvalDao.isApproved(wardUserId, type);
    }

    /**
     * Option 3: Medical Emergency with Document Submission and AI-assisted Verification.
     */
    public MedicalVerificationService.VerificationResult processMedicalEmergencySubmission(
            int userId, String hospitalName, String doctorRegistration, String diagnosisText, String notes) {

        MedicalVerificationService.VerificationResult result =
                medicalVerificationService.verifyDocument(hospitalName, doctorRegistration, diagnosisText, notes);

        if (result.isApproved()) {
            approvalDao.setApproval(userId, ApprovalRequest.Type.MEDICAL, ApprovalRequest.Status.APPROVED,
                    "AI Verified Doctor Report", result.getDiagnosticSummary());
        } else {
            approvalDao.setApproval(userId, ApprovalRequest.Type.MEDICAL, ApprovalRequest.Status.REJECTED,
                    "AI Verification Failed", result.getFailureReason());
        }

        return result;
    }

    /**
     * Option 4: AI Educational Study Tasks verification.
     */
    public boolean grantTaskApproval(int userId, String taskSummary) {
        approvalDao.setApproval(userId, ApprovalRequest.Type.TASK, ApprovalRequest.Status.APPROVED,
                "Completed required educational study curriculum", taskSummary);
        return true;
    }

    /**
     * Option 5: Release Coins.
     * Redeems available Release Coins (at 1 Coin = ₹1 / $1) to unlock early release.
     */
    public boolean redeemCoinsForEarlyRelease(int userId, double coinsToRedeem) {
        if (coinsToRedeem < 10.0) {
            return false; // Minimum coin threshold for early release authority
        }

        boolean redeemed = releaseCoinService.redeemCoinsForRelease(userId, coinsToRedeem);
        if (redeemed) {
            approvalDao.setApproval(userId, ApprovalRequest.Type.COIN, ApprovalRequest.Status.APPROVED,
                    "Redeemed " + coinsToRedeem + " Release Coins", "1 Coin = ₹1.00");
            return true;
        }
        return false;
    }

    /**
     * Determines whether early withdrawal is legally authorized according to the 5 rules.
     */
    public boolean isEarlyWithdrawalAuthorized(int userId, double requestedAmount) {
        Wallet wallet = walletDao.findByUserId(userId).orElse(null);
        if (wallet == null) return false;

        // If target reached, direct withdrawal is unconditionally unlocked
        if (wallet.isTargetCompleted() || wallet.getAvailableBalance() >= wallet.getTargetBalance()) {
            return true;
        }

        // Otherwise, requires at least 1 of the 5 active approvals
        return isParentApproved(userId)
                || isFriendApproved(userId)
                || isMedicalApproved(userId)
                || isTaskApproved(userId)
                || isCoinApproved(userId);
    }
}
