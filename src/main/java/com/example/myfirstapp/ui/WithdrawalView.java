package com.example.myfirstapp.ui;

import com.example.myfirstapp.model.User;
import com.example.myfirstapp.model.Wallet;
import com.example.myfirstapp.service.AuthService;
import com.example.myfirstapp.service.EarlyReleaseService;
import com.example.myfirstapp.service.MedicalVerificationService;
import com.example.myfirstapp.service.ReleaseCoinService;
import com.example.myfirstapp.service.WalletService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.Locale;

/**
 * Recreates WithdrawalActivity.java & activity_withdrawal.xml in JavaFX.
 * Preserves the 5 early-release options and enforces business logic rules.
 * Shows uniform withdrawable hint text format and displays alerts prominently at the top.
 */
public class WithdrawalView extends VBox {
    private final AuthService authService = AuthService.getInstance();
    private final WalletService walletService = new WalletService();
    private final EarlyReleaseService earlyReleaseService = new EarlyReleaseService();
    private final ReleaseCoinService releaseCoinService = new ReleaseCoinService();

    private final Label balanceInfoText;
    private final Label withdrawableHintLabel;
    private final Label targetStatusText;
    private final TextField withdrawAmountField;
    private final Label parentStatusText;
    private final Label friendStatusText;
    private final Label medicalStatusText;
    private final Label taskStatusText;
    private final Label coinsStatusText;

    private final HBox feedbackBanner;
    private final Label feedbackIcon;
    private final Label feedbackLabel;

    public WithdrawalView() {
        setSpacing(12);
        setPadding(new Insets(14, 16, 20, 16));
        setStyle("-fx-background-color: #E0F7FA;");

        // Top Navigation Bar
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button();
        backBtn.setGraphic(IconHelper.getBackArrowIcon(Color.web("#263238"), 1.2));
        backBtn.getStyleClass().add("back-button");
        backBtn.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20px; -fx-padding: 8px; -fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 2);");
        backBtn.setOnAction(e -> ScreenManager.getInstance().goBack());

        Label title = new Label("Withdrawal Hub");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #263238;");

        topBar.getChildren().addAll(backBtn, title);

        // TOP Alert / Feedback Banner (Prominently displayed at top, never hidden at bottom)
        feedbackBanner = new HBox(10);
        feedbackBanner.setAlignment(Pos.CENTER_LEFT);
        feedbackBanner.setPadding(new Insets(10, 16, 10, 16));
        feedbackBanner.setStyle("-fx-background-color: #FFEBEE; -fx-background-radius: 16px; -fx-border-color: #FFCDD2; -fx-border-radius: 16px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);");
        feedbackBanner.setVisible(false);
        feedbackBanner.setManaged(false);

        feedbackIcon = new Label("⚠️");
        feedbackIcon.setStyle("-fx-font-size: 14px;");

        feedbackLabel = new Label("");
        feedbackLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #C62828; -fx-wrap-text: true;");
        HBox.setHgrow(feedbackLabel, Priority.ALWAYS);

        feedbackBanner.getChildren().addAll(feedbackIcon, feedbackLabel);

        VBox contentBox = new VBox(14);
        contentBox.setStyle("-fx-background-color: transparent;");

        // 1. Balance Info Card (Clean 20px rounded edges)
        VBox balanceCard = new VBox(8);
        balanceCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20px; -fx-border-radius: 20px; " +
                "-fx-border-color: #B2EBF2; -fx-padding: 18px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0.05, 0, 3);");

        balanceInfoText = new Label("Available: ₹65.00 | Locked: ₹40.00 | Target: ₹200.00");
        balanceInfoText.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #263238;");

        withdrawableHintLabel = new Label("💡 ₹65.00 can be withdrawn (unlocked by 65 Release Coins)");
        withdrawableHintLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #00838F; -fx-wrap-text: true;");

        targetStatusText = new Label("Target not reached! Early withdrawal requires 1 of 5 approval methods.");
        targetStatusText.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #C10C4E; -fx-wrap-text: true;");
        targetStatusText.setVisible(false);
        targetStatusText.setManaged(false);

        HBox targetBar = new HBox(10);
        targetBar.setAlignment(Pos.CENTER_LEFT);
        Button btnAdjustTarget = new Button("🎯 Adjust Savings Target");
        btnAdjustTarget.setStyle("-fx-background-color: #E0F7FA; -fx-text-fill: #00838F; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 12px; -fx-padding: 6px 14px; -fx-font-size: 12px;");
        btnAdjustTarget.setOnAction(e -> new TargetManipulatorDialog(this::refreshStatus).show());
        targetBar.getChildren().add(btnAdjustTarget);

        balanceCard.getChildren().addAll(balanceInfoText, withdrawableHintLabel, targetStatusText, targetBar);

        // 2. Withdrawal Input Card (Clean 20px rounded edges)
        VBox inputCard = new VBox(10);
        inputCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20px; -fx-border-radius: 20px; " +
                "-fx-border-color: #B2EBF2; -fx-padding: 18px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0.05, 0, 3);");

        Label withdrawLabel = new Label("Withdrawal Amount (₹)");
        withdrawLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #00838F;");

        withdrawAmountField = new TextField("65.00");
        withdrawAmountField.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #263238; -fx-background-color: #F5F7F8; -fx-background-radius: 14px; -fx-border-color: #CFD8DC; -fx-border-radius: 14px; -fx-padding: 10px 14px;");

        Button btnWithdraw = new Button("Execute Withdrawal");
        btnWithdraw.getStyleClass().add("cyan-button");
        btnWithdraw.setStyle("-fx-background-color: #00BCD4; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; " +
                "-fx-background-radius: 16px; -fx-padding: 12px 24px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0, 188, 212, 0.3), 8, 0.1, 0, 3);");
        btnWithdraw.setPrefWidth(Double.MAX_VALUE);
        btnWithdraw.setOnAction(e -> handleExecuteWithdrawal());

        inputCard.getChildren().addAll(withdrawLabel, withdrawAmountField, btnWithdraw);

        // 3. Section Title: 5 Early Release & Emergency Options
        Label sectionTitle = new Label("5 Early Release & Emergency Options");
        sectionTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #263238;");

        // Option 1: Parent Approval
        VBox op1 = new VBox(8);
        op1.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16px; -fx-border-radius: 16px; -fx-border-color: #E0F2F1; -fx-padding: 14px;");
        parentStatusText = new Label("1. Parent Approval: Not Granted");
        parentStatusText.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #455A64;");
        Button btnParent = new Button("Request / Manage Parent Approval");
        btnParent.setStyle("-fx-background-color: #00BCD4; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 12px; -fx-padding: 6px 14px;");
        btnParent.setOnAction(e -> ScreenManager.getInstance().showGuardian());
        op1.getChildren().addAll(parentStatusText, btnParent);

        // Option 2: Close Friend Approval
        VBox op2 = new VBox(8);
        op2.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16px; -fx-border-radius: 16px; -fx-border-color: #E0F2F1; -fx-padding: 14px;");
        friendStatusText = new Label("2. Close Friend Approval: Not Granted");
        friendStatusText.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #455A64;");
        Button btnFriend = new Button("Request Friend Approval");
        btnFriend.setStyle("-fx-background-color: #00BCD4; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 12px; -fx-padding: 6px 14px;");
        btnFriend.setOnAction(e -> ScreenManager.getInstance().showGuardian());
        op2.getChildren().addAll(friendStatusText, btnFriend);

        // Option 3: Medical Emergency AI Verification
        VBox op3 = new VBox(8);
        op3.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16px; -fx-border-radius: 16px; -fx-border-color: #E0F2F1; -fx-padding: 14px;");
        medicalStatusText = new Label("3. Medical Emergency (AI Verified): Not Verified");
        medicalStatusText.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #455A64;");
        Button btnMedical = new Button("Upload & AI Verify Doctor's Report");
        btnMedical.setStyle("-fx-background-color: #00BCD4; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 12px; -fx-padding: 6px 14px;");
        btnMedical.setOnAction(e -> openMedicalUploadDialog());
        op3.getChildren().addAll(medicalStatusText, btnMedical);

        // Option 4: AI Educational Tasks
        VBox op4 = new VBox(8);
        op4.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16px; -fx-border-radius: 16px; -fx-border-color: #E0F2F1; -fx-padding: 14px;");
        taskStatusText = new Label("4. AI Educational Study Tasks: Incomplete");
        taskStatusText.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #455A64;");
        Button btnTasks = new Button("Complete Study Tasks");
        btnTasks.setStyle("-fx-background-color: #00BCD4; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 12px; -fx-padding: 6px 14px;");
        btnTasks.setOnAction(e -> ScreenManager.getInstance().showStudyTask());
        op4.getChildren().addAll(taskStatusText, btnTasks);

        // Option 5: Release Coins
        VBox op5 = new VBox(8);
        op5.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16px; -fx-border-radius: 16px; -fx-border-color: #E0F2F1; -fx-padding: 14px;");
        coinsStatusText = new Label("5. Release Coins: 65 Coins Available");
        coinsStatusText.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #455A64;");
        Button btnCoins = new Button("Use Release Coins For Early Release (10 Coins)");
        btnCoins.setStyle("-fx-background-color: #00BCD4; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 12px; -fx-padding: 6px 14px;");
        btnCoins.setOnAction(e -> handleRedeemCoins());
        op5.getChildren().addAll(coinsStatusText, btnCoins);

        contentBox.getChildren().addAll(balanceCard, inputCard, sectionTitle, op1, op2, op3, op4, op5);

        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-hbar-policy: never;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(topBar, feedbackBanner, scrollPane);

        refreshStatus();
    }

    public void showFeedback(String message, boolean isError) {
        feedbackLabel.setText(message);
        if (isError) {
            feedbackBanner.setStyle("-fx-background-color: #FFEBEE; -fx-background-radius: 16px; -fx-border-color: #FFCDD2; -fx-border-radius: 16px; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);");
            feedbackIcon.setText("⚠️");
            feedbackLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #C62828; -fx-wrap-text: true;");
        } else {
            feedbackBanner.setStyle("-fx-background-color: #E8F5E9; -fx-background-radius: 16px; -fx-border-color: #A5D6A7; -fx-border-radius: 16px; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);");
            feedbackIcon.setText("✅");
            feedbackLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2E7D32; -fx-wrap-text: true;");
        }
        feedbackBanner.setVisible(true);
        feedbackBanner.setManaged(true);
    }

    public void refreshStatus() {
        User user = authService.getCurrentUser();
        if (user == null) return;

        Wallet wallet = walletService.getWallet(user.getId());
        double totalBalance = wallet.getTotalBalance();
        double effective = wallet.getEffectiveBalance();
        double effectiveLocked = wallet.getEffectiveLockedSavings();
        double target = wallet.getTargetBalance();
        double coins = wallet.getReleaseCoins();

        boolean parentOk = earlyReleaseService.isParentApproved(user.getId());
        boolean friendOk = earlyReleaseService.isFriendApproved(user.getId());
        boolean medicalOk = earlyReleaseService.isMedicalApproved(user.getId());
        boolean taskOk = earlyReleaseService.isTaskApproved(user.getId());
        boolean coinOk = earlyReleaseService.isCoinApproved(user.getId());

        balanceInfoText.setText(String.format(Locale.getDefault(),
                "Available: ₹%.2f | Locked: ₹%.2f | Target: ₹%.2f",
                effective, effectiveLocked, target));

        withdrawableHintLabel.setText(wallet.getWithdrawableHint());

        if (wallet.isTargetCompleted()) {
            targetStatusText.setText("Target Achieved! Direct withdrawal is fully unlocked.");
            targetStatusText.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2E7D32; -fx-wrap-text: true;");
        } else {
            targetStatusText.setText("Target not reached! Early withdrawal requires 1 of 5 approval methods.");
            targetStatusText.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #C10C4E; -fx-wrap-text: true;");
        }

        targetStatusText.setVisible(false);
        targetStatusText.setManaged(false);

        parentStatusText.setText("1. Parent Approval: " + (parentOk ? "APPROVED" : "Not Granted"));
        parentStatusText.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + (parentOk ? "#2E7D32;" : "#455A64;"));

        friendStatusText.setText("2. Close Friend Approval: " + (friendOk ? "APPROVED" : "Not Granted"));
        friendStatusText.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + (friendOk ? "#2E7D32;" : "#455A64;"));

        medicalStatusText.setText("3. Medical Emergency (AI Verified): " + (medicalOk ? "APPROVED" : "Not Verified"));
        medicalStatusText.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + (medicalOk ? "#2E7D32;" : "#455A64;"));

        taskStatusText.setText("4. AI Educational Study Tasks: " + (taskOk ? "COMPLETED" : "Incomplete"));
        taskStatusText.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + (taskOk ? "#2E7D32;" : "#455A64;"));

        coinsStatusText.setText(String.format(Locale.getDefault(), "5. Release Coins: %.0f Coins Available (cancels ₹%.0f early withdrawal lock)", coins, coins));
        coinsStatusText.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + (coins >= 10 ? "#00838F;" : "#455A64;"));
    }

    /** Shown only after a rejected withdrawal attempt while the savings target is still unmet. */
    private void showTargetWarning(Wallet wallet) {
        if (wallet.isTargetCompleted()) return;
        targetStatusText.setText("Target not reached! Early withdrawal requires 1 of 5 approval methods.");
        targetStatusText.setVisible(true);
        targetStatusText.setManaged(true);
    }

    private void handleExecuteWithdrawal() {
        String input = withdrawAmountField.getText().trim();
        double amount;
        try {
            amount = Double.parseDouble(input);
            if (amount <= 0.0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showFeedback("Please enter a valid positive withdrawal amount!", true);
            return;
        }

        User user = authService.getCurrentUser();
        if (user == null) return;

        Wallet wallet = walletService.getWallet(user.getId());
        if (amount > wallet.getEffectiveBalance()) {
            showFeedback(String.format(Locale.getDefault(),
                    "Amount exceeds withdrawable balance! Available: ₹%.2f (Locked: ₹%.2f)",
                    wallet.getEffectiveBalance(), wallet.getEffectiveLockedSavings()), true);
            showTargetWarning(wallet);
            return;
        }

        boolean authorized = earlyReleaseService.isEarlyWithdrawalAuthorized(user.getId(), amount);
        if (!authorized && wallet.getReleaseCoins() >= 10.0 && amount <= wallet.getEffectiveBalance()) {
            earlyReleaseService.redeemCoinsForEarlyRelease(user.getId(), Math.min(wallet.getReleaseCoins(), Math.max(10.0, amount)));
            authorized = true;
        }
        if (!authorized) {
            showFeedback("Early withdrawal locked! Please complete 1 of the 5 emergency release channels.", true);
            showTargetWarning(wallet);
            return;
        }

        double coinsBefore = wallet.getReleaseCoins();
        boolean withdrawn = walletService.withdrawFunds(user.getId(), amount, true);
        if (withdrawn) {
            Wallet updated = walletService.getWallet(user.getId());
            double coinsSpent = Math.max(0.0, coinsBefore - updated.getReleaseCoins());
            if (coinsSpent > 0) {
                showFeedback(String.format(Locale.getDefault(),
                        "Successfully withdrawn ₹%.2f! Spent %.0f Release Coins (%.0f remaining).",
                        amount, coinsSpent, updated.getReleaseCoins()), false);
            } else {
                showFeedback(String.format(Locale.getDefault(),
                        "Successfully withdrawn ₹%.2f from your wallet!", amount), false);
            }
            refreshStatus();
            withdrawAmountField.setText("");
        }
    }

    private void handleRedeemCoins() {
        User user = authService.getCurrentUser();
        if (user == null) return;

        Wallet wallet = walletService.getWallet(user.getId());
        if (wallet.getLockedSavings() <= 0.0) {
            showFeedback("No locked savings remaining! Target already reached or fully unlocked.", false);
            return;
        }

        if (wallet.getReleaseCoins() < 10.0) {
            showFeedback("Insufficient coins! You need at least 10 Release Coins.", true);
            return;
        }

        boolean success = earlyReleaseService.redeemCoinsForEarlyRelease(user.getId(), 10.0);
        if (success) {
            showFeedback("Redeemed 10 Release Coins! Unlocked ₹10.00 of locked savings into available balance.", false);
            refreshStatus();
        } else {
            showFeedback("Could not redeem coins. Please ensure you have eligible locked savings.", true);
        }
    }

    private void openMedicalUploadDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Medical Emergency Verification Prototype");
        dialog.setHeaderText("AI-Assisted Doctor Report Verification");

        VBox form = new VBox(10);
        form.setPadding(new Insets(16));

        TextField hospField = new TextField("Metro Emergency Hospital");
        hospField.setPromptText("Hospital / Medical Institution Name");

        TextField docRegField = new TextField("MCI-84920");
        docRegField.setPromptText("Doctor Registration No (e.g. MCI-84920)");

        TextArea diagArea = new TextArea("Acute appendicitis with high fever requiring emergency laparoscopy surgery and ICU hospitalization.");
        diagArea.setPrefRowCount(3);
        diagArea.setWrapText(true);

        Label resultLabel = new Label();
        resultLabel.setStyle("-fx-font-weight: bold;");

        Button submitBtn = new Button("Run AI Diagnostic Verification");
        submitBtn.setStyle("-fx-background-color: #00BCD4; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 12px; -fx-padding: 8px 16px; -fx-cursor: hand;");
        submitBtn.setOnAction(e -> {
            User user = authService.getCurrentUser();
            if (user != null) {
                MedicalVerificationService.VerificationResult result = earlyReleaseService.processMedicalEmergencySubmission(
                        user.getId(), hospField.getText(), docRegField.getText(), diagArea.getText(), "Emergency admission");

                if (result.isApproved()) {
                    resultLabel.setText("✅ " + result.getDiagnosticSummary());
                    resultLabel.setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
                    refreshStatus();
                    showFeedback("Medical Emergency Verified! Early release authorized.", false);
                } else {
                    resultLabel.setText("❌ " + result.getFailureReason());
                    resultLabel.setStyle("-fx-text-fill: #C62828; -fx-font-weight: bold;");
                    showFeedback("Medical verification failed: " + result.getFailureReason(), true);
                }
            }
        });

        form.getChildren().addAll(
                new Label("Hospital / Institution:"), hospField,
                new Label("Doctor Registration No:"), docRegField,
                new Label("Clinical Diagnosis & Severity:"), diagArea,
                submitBtn, resultLabel
        );

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        dialog.showAndWait();
    }
}
