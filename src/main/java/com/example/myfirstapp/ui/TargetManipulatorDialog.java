package com.example.myfirstapp.ui;

import com.example.myfirstapp.model.Guardian;
import com.example.myfirstapp.model.User;
import com.example.myfirstapp.model.Wallet;
import com.example.myfirstapp.service.AuthService;
import com.example.myfirstapp.service.ConnectService;
import com.example.myfirstapp.service.WalletService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;
import java.util.Locale;

/**
 * Savings Target Manipulator Dialog.
 * Enforces the strict SmartSave rule: Target cannot be changed without Guardian permission.
 * Displays registered guardians and requires explicit guardian approval.
 */
public class TargetManipulatorDialog {
    private final AuthService authService = AuthService.getInstance();
    private final WalletService walletService = new WalletService();
    private final ConnectService connectService = new ConnectService();
    private final Runnable onUpdated;

    public TargetManipulatorDialog(Runnable onUpdated) {
        this.onUpdated = onUpdated;
    }

    public void show() {
        User user = authService.getCurrentUser();
        if (user == null) return;

        Wallet wallet = walletService.getWallet(user.getId());
        List<Guardian> guardians = connectService.getGuardians(user.getId());
        boolean hasGuardian = !guardians.isEmpty();
        boolean isPreApproved = walletService.isGuardianTargetPermissionGranted(user.getId());

        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Target Manipulator — SmartSave");

        VBox layout = new VBox(14);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #E0F7FA;");
        layout.setPrefWidth(420);

        // 1. Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("🎯 Savings Target Manipulator");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #00838F;");
        header.getChildren().add(title);

        Label subtitle = new Label("SmartSave Financial Discipline: Target cannot be changed without Guardian permission.");
        subtitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #37474F; -fx-wrap-text: true;");

        // 2. Current Status Card
        VBox infoCard = new VBox(6);
        infoCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12px; -fx-padding: 12px;");

        Label currentTargetLabel = new Label(String.format(Locale.getDefault(), "Current Target: ₹%.2f", wallet.getTargetBalance()));
        currentTargetLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #263238;");

        Label totalBalanceLabel = new Label(String.format(Locale.getDefault(), "Total Balance: ₹%.2f", wallet.getTotalBalance()));
        totalBalanceLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #00838F;");

        Label coinLabel = new Label(String.format(Locale.getDefault(), "Release Coins: %.0f Coins", wallet.getReleaseCoins()));
        coinLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #FF9800;");

        infoCard.getChildren().addAll(currentTargetLabel, totalBalanceLabel, coinLabel);

        // 3. Guardian Permission Card (MANDATORY)
        VBox guardianCard = new VBox(8);
        guardianCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12px; -fx-padding: 12px; -fx-border-color: #00BCD4; -fx-border-radius: 12px;");

        Label guardianTitle = new Label("🔒 Guardian Permission (Required)");
        guardianTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #00838F;");

        CheckBox guardianConsentCheck = new CheckBox();
        guardianConsentCheck.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #263238; -fx-wrap-text: true;");

        Label guardianStatusDesc = new Label();
        guardianStatusDesc.setStyle("-fx-font-size: 12px; -fx-wrap-text: true;");

        if (!hasGuardian) {
            guardianConsentCheck.setText("No Guardian Registered");
            guardianConsentCheck.setDisable(true);
            guardianStatusDesc.setText("⚠️ You do not have a registered Guardian. SmartSave requires Guardian permission before any target can be changed. Please register a Parent or Guardian in the Guardian section first.");
            guardianStatusDesc.setStyle("-fx-text-fill: #C10C4E; -fx-font-weight: bold;");
        } else {
            Guardian primary = guardians.get(0);
            String guardianName = primary.getName() + " (" + primary.getRelationship() + ")";
            guardianConsentCheck.setText("I (" + guardianName + ") grant permission to change target");
            guardianConsentCheck.setSelected(isPreApproved);

            if (isPreApproved) {
                guardianStatusDesc.setText("✅ Guardian permission is currently ACTIVE in your Guardian & Approval Hub.");
                guardianStatusDesc.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
            } else {
                guardianStatusDesc.setText("Guardian must approve this adjustment to prevent impulsive commitment changes.");
                guardianStatusDesc.setStyle("-fx-text-fill: #546E7A;");
            }
        }

        guardianCard.getChildren().addAll(guardianTitle, guardianConsentCheck, guardianStatusDesc);

        // 4. Input Section
        VBox inputSection = new VBox(8);
        Label inputLabel = new Label("New Savings Target (₹):");
        inputLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #37474F;");

        TextField targetField = new TextField(String.format(Locale.getDefault(), "%.2f", wallet.getTargetBalance()));
        targetField.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 8px;");

        // Quick presets
        HBox presets = new HBox(6);
        presets.setAlignment(Pos.CENTER);
        presets.getChildren().addAll(
                createPresetButton("₹100", 100.0, targetField),
                createPresetButton("₹150", 150.0, targetField),
                createPresetButton("₹200", 200.0, targetField),
                createPresetButton("₹300", 300.0, targetField),
                createPresetButton("₹500", 500.0, targetField)
        );

        Label previewLabel = new Label();
        previewLabel.setStyle("-fx-font-size: 12px; -fx-wrap-text: true; -fx-text-fill: #00796B;");
        updatePreview(previewLabel, wallet, targetField.getText());

        targetField.textProperty().addListener((obs, oldVal, newVal) -> updatePreview(previewLabel, wallet, newVal));

        inputSection.getChildren().addAll(inputLabel, targetField, presets, previewLabel);

        // 5. Action Buttons
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #B0BEC5; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-cursor: hand; -fx-padding: 8px 16px;");
        cancelBtn.setOnAction(e -> dialogStage.close());

        Button saveBtn = new Button("Apply Target");
        saveBtn.setStyle("-fx-background-color: #00BCD4; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-cursor: hand; -fx-padding: 8px 16px;");
        if (!hasGuardian) {
            saveBtn.setDisable(true);
        }

        saveBtn.setOnAction(e -> {
            if (!hasGuardian) {
                previewLabel.setText("❌ Target cannot be changed without a registered Guardian!");
                previewLabel.setStyle("-fx-text-fill: #C10C4E; -fx-font-weight: bold;");
                return;
            }

            if (!guardianConsentCheck.isSelected()) {
                previewLabel.setText("⚠️ Target cannot be changed without Guardian permission! Please check the Guardian authorization box.");
                previewLabel.setStyle("-fx-text-fill: #C10C4E; -fx-font-weight: bold;");
                return;
            }

            try {
                double newTarget = Double.parseDouble(targetField.getText().trim());
                if (newTarget < 0.0) {
                    previewLabel.setText("Target cannot be negative!");
                    previewLabel.setStyle("-fx-text-fill: #C10C4E;");
                    return;
                }

                // Grant and persist guardian target permission
                String guardianName = guardians.get(0).getName();
                walletService.setGuardianTargetPermission(user.getId(), true, guardianName);

                boolean success = walletService.updateSavingsTarget(user.getId(), newTarget, true);
                if (success) {
                    if (onUpdated != null) {
                        onUpdated.run();
                    }
                    dialogStage.close();
                } else {
                    previewLabel.setText("Failed to update target. Guardian permission check failed.");
                    previewLabel.setStyle("-fx-text-fill: #C10C4E;");
                }
            } catch (NumberFormatException ex) {
                previewLabel.setText("Please enter a valid numeric target amount.");
                previewLabel.setStyle("-fx-text-fill: #C10C4E;");
            }
        });

        buttonBar.getChildren().addAll(cancelBtn, saveBtn);

        layout.getChildren().addAll(header, subtitle, infoCard, guardianCard, inputSection, buttonBar);

        Scene scene = new Scene(layout);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    private Button createPresetButton(String text, double value, TextField field) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #00838F; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand; -fx-border-color: #B2EBF2; -fx-border-radius: 6px;");
        btn.setOnAction(e -> field.setText(String.format(Locale.getDefault(), "%.2f", value)));
        return btn;
    }

    private void updatePreview(Label previewLabel, Wallet wallet, String text) {
        try {
            double newTarget = Double.parseDouble(text.trim());
            double total = wallet.getTotalBalance();
            if (newTarget <= 0.0) {
                previewLabel.setText("Target cleared: All funds will be fully unlocked.");
                previewLabel.setStyle("-fx-text-fill: #4CAF50;");
            } else if (total >= newTarget) {
                previewLabel.setText(String.format(Locale.getDefault(),
                        "🎉 Target Achieved! Total balance (₹%.2f) >= Target (₹%.2f). All savings will be unlocked!",
                        total, newTarget));
                previewLabel.setStyle("-fx-text-fill: #4CAF50;");
            } else {
                double coins = wallet.getReleaseCoins();
                double unlockable = Math.min(total, coins);
                double locked = Math.max(0.0, total - unlockable);
                previewLabel.setText(String.format(Locale.getDefault(),
                        "Target Incomplete: ₹%.2f withdrawable (unlocked by %.0f coins); ₹%.2f locked toward target of ₹%.2f.",
                        unlockable, coins, locked, newTarget));
                previewLabel.setStyle("-fx-text-fill: #00838F;");
            }
        } catch (Exception e) {
            previewLabel.setText("Enter a target amount.");
            previewLabel.setStyle("-fx-text-fill: #78909C;");
        }
    }
}
