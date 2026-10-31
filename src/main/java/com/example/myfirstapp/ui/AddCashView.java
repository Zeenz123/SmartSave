package com.example.myfirstapp.ui;

import com.example.myfirstapp.model.User;
import com.example.myfirstapp.model.Wallet;
import com.example.myfirstapp.service.AuthService;
import com.example.myfirstapp.service.WalletService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.Locale;

/**
 * Recreates AddCashActivity.java & activity_add_cash.xml in JavaFX.
 * Allows simulated virtual deposit (parent-to-child or virtual top-up) with quick-select buttons.
 */
public class AddCashView extends VBox {
    private final AuthService authService = AuthService.getInstance();
    private final WalletService walletService = new WalletService();
    private final TextField amountField;
    private final Label feedbackLabel;

    public AddCashView() {
        setSpacing(18);
        setPadding(new Insets(24, 20, 24, 20));
        setStyle("-fx-background-color: #E0F7FA;");

        // Top Navigation Bar
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button();
        backBtn.setGraphic(IconHelper.getBackArrowIcon(Color.web("#263238"), 1.2));
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> ScreenManager.getInstance().goBack());

        Label title = new Label("Add Cash to Wallet");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #263238;");

        topBar.getChildren().addAll(backBtn, title);

        Label subtitle = new Label("Enter amount to add into your virtual account");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #607D8B;");

        User user = authService.getCurrentUser();
        Wallet wallet = user != null ? walletService.getWallet(user.getId()) : new Wallet(0, 0, 0, 0, 0);
        Label withdrawHint = new Label(wallet.getWithdrawableHint());
        withdrawHint.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #00838F;");

        // Deposit Amount Card
        VBox depositCard = new VBox(10);
        depositCard.getStyleClass().add("white-card");
        depositCard.setAlignment(Pos.CENTER);
        depositCard.setPadding(new Insets(24));

        Label cardHeader = new Label("Deposit Amount");
        cardHeader.setStyle("-fx-font-size: 14px; -fx-text-fill: #607D8B;");

        HBox amountRow = new HBox(8);
        amountRow.setAlignment(Pos.CENTER);

        Label rupeeSign = new Label("₹");
        rupeeSign.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #00BCD4;");

        amountField = new TextField("50.00");
        amountField.setStyle("-fx-font-size: 34px; -fx-font-weight: bold; -fx-text-fill: #000000; -fx-background-color: transparent; -fx-alignment: center;");
        amountField.setPrefWidth(160);

        amountRow.getChildren().addAll(rupeeSign, amountField);
        depositCard.getChildren().addAll(cardHeader, amountRow);

        // Quick Select Section
        Label quickSelectLabel = new Label("Quick Select");
        quickSelectLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #263238;");

        HBox quickButtons = new HBox(8);
        quickButtons.setAlignment(Pos.CENTER);

        Button b10 = createQuickButton("+₹10", "10.00");
        Button b25 = createQuickButton("+₹25", "25.00");
        Button b50 = createQuickButton("+₹50", "50.00");
        Button b100 = createQuickButton("+₹100", "100.00");

        HBox.setHgrow(b10, Priority.ALWAYS);
        HBox.setHgrow(b25, Priority.ALWAYS);
        HBox.setHgrow(b50, Priority.ALWAYS);
        HBox.setHgrow(b100, Priority.ALWAYS);

        quickButtons.getChildren().addAll(b10, b25, b50, b100);

        // Payment Methods: Google Pay & NetBanking
        Label payMethodLabel = new Label("Payment Method");
        payMethodLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #263238;");

        TextField upiIdField = new TextField("alex@okaxis");
        upiIdField.setPromptText("Enter Google Pay UPI ID (e.g. name@okaxis)");
        upiIdField.setStyle("-fx-font-size: 14px; -fx-background-color: #FFFFFF; -fx-background-radius: 10px; -fx-border-color: #CFD8DC; -fx-border-radius: 10px; -fx-padding: 8px 12px;");

        feedbackLabel = new Label("");
        feedbackLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        // Google Pay Branded Button
        Button gpayBtn = new Button("⚡ Pay with Google Pay (GPay)");
        gpayBtn.setStyle("-fx-background-color: #1F1F1F; -fx-text-fill: #FFFFFF; -fx-font-size: 15px; -fx-font-weight: bold; -fx-background-radius: 12px; -fx-padding: 12px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 8, 0, 0, 2);");
        gpayBtn.setPrefWidth(Double.MAX_VALUE);
        gpayBtn.setOnAction(e -> handleAddCash(upiIdField.getText().trim(), true));

        Button addCashBtn = new Button("Add Cash via Bank / UPI");
        addCashBtn.getStyleClass().add("cyan-button");
        addCashBtn.setPrefWidth(Double.MAX_VALUE);
        addCashBtn.setPrefHeight(46);
        addCashBtn.setOnAction(e -> handleAddCash(upiIdField.getText().trim(), false));

        getChildren().addAll(topBar, subtitle, withdrawHint, depositCard, quickSelectLabel, quickButtons, payMethodLabel, upiIdField, feedbackLabel, gpayBtn, addCashBtn);
    }

    private Button createQuickButton(String label, String amount) {
        Button btn = new Button(label);
        btn.setStyle("-fx-background-color: #00BCD4; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-background-radius: 10px; -fx-cursor: hand;");
        btn.setPrefHeight(38);
        btn.setOnAction(e -> amountField.setText(amount));
        return btn;
    }

    private void handleAddCash(String upiId, boolean isGooglePay) {
        String input = amountField.getText().trim();
        double amount;
        try {
            amount = Double.parseDouble(input);
        } catch (NumberFormatException e) {
            feedbackLabel.setText("Please enter a valid amount!");
            feedbackLabel.setStyle("-fx-text-fill: #C10C4E;");
            return;
        }

        if (amount <= 0) {
            feedbackLabel.setText("Amount must be greater than ₹0.00");
            feedbackLabel.setStyle("-fx-text-fill: #C10C4E;");
            return;
        }

        User user = authService.getCurrentUser();
        if (user != null) {
            String desc = isGooglePay
                    ? "Google Pay UPI Deposit - " + (upiId == null || upiId.isBlank() ? "user@okaxis" : upiId)
                    : "Deposit via Bank / UPI - " + (upiId == null || upiId.isBlank() ? "Direct TopUp" : upiId);
            boolean success = walletService.depositVirtualFunds(user.getId(), amount, desc);
            if (success) {
                feedbackLabel.setText(String.format(Locale.getDefault(), "Successfully added ₹%.2f via %s!", amount, isGooglePay ? "Google Pay" : "NetBanking"));
                feedbackLabel.setStyle("-fx-text-fill: #4CAF50;");
                ScreenManager.getInstance().showHome();
            } else {
                feedbackLabel.setText("Deposit failed due to a database error.");
                feedbackLabel.setStyle("-fx-text-fill: #C10C4E;");
            }
        }
    }
}
