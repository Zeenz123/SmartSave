package com.example.myfirstapp.ui;

import com.example.myfirstapp.model.User;
import com.example.myfirstapp.model.Wallet;
import com.example.myfirstapp.service.AuthService;
import com.example.myfirstapp.service.ConnectService;
import com.example.myfirstapp.service.WalletService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.Locale;

/**
 * Recreates RequestPaymentActivity.java & activity_request_payment.xml in JavaFX.
 */
public class RequestPaymentView extends VBox {
    private final AuthService authService = AuthService.getInstance();
    private final ConnectService connectService = new ConnectService();

    private final TextField contactField;
    private final TextField amountField;
    private final Label statusLabel;

    public RequestPaymentView() {
        setSpacing(18);
        setPadding(new Insets(24, 20, 24, 20));
        setStyle("-fx-background-color: #E0F7FA;");

        // Top Bar
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button();
        backBtn.setGraphic(IconHelper.getBackArrowIcon(Color.web("#263238"), 1.2));
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> ScreenManager.getInstance().goBack());

        Label title = new Label("Request Payment");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #263238;");

        topBar.getChildren().addAll(backBtn, title);

        // Card Container
        VBox card = new VBox(14);
        card.getStyleClass().add("white-card");
        card.setPadding(new Insets(20));

        Label contactHeader = new Label("Request Money From");
        contactHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #00BCD4;");

        contactField = new TextField();
        contactField.setPromptText("Contact Name / Phone / Username");
        contactField.getStyleClass().add("text-input-field");

        Label amountHeader = new Label("Requested Amount (₹)");
        amountHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #00BCD4;");

        amountField = new TextField();
        amountField.setPromptText("0.00");
        amountField.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #263238; -fx-background-color: #F5F7F8; -fx-background-radius: 12px; -fx-padding: 10px;");

        User user = authService.getCurrentUser();
        Wallet wallet = user != null ? new WalletService().getWallet(user.getId()) : new Wallet(0, 0, 0, 0, 0);
        Label withdrawHint = new Label(wallet.getWithdrawableHint());
        withdrawHint.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #00838F;");

        Button requestBtn = new Button("Send Payment Request");
        requestBtn.getStyleClass().add("cyan-button");
        requestBtn.setPrefWidth(Double.MAX_VALUE);
        requestBtn.setPrefHeight(52);
        requestBtn.setOnAction(e -> handleRequest());

        card.getChildren().addAll(contactHeader, contactField, amountHeader, amountField, withdrawHint, requestBtn);

        statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        getChildren().addAll(topBar, card, statusLabel);
    }

    private void handleRequest() {
        String contact = contactField.getText().trim();
        String amountStr = amountField.getText().trim();

        if (contact.isEmpty()) {
            statusLabel.setText("Please enter contact details!");
            statusLabel.setStyle("-fx-text-fill: #C10C4E;");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            statusLabel.setText("Please enter a valid amount!");
            statusLabel.setStyle("-fx-text-fill: #C10C4E;");
            return;
        }

        if (amount <= 0) {
            statusLabel.setText("Amount must be greater than ₹0.00");
            statusLabel.setStyle("-fx-text-fill: #C10C4E;");
            return;
        }

        User user = authService.getCurrentUser();
        if (user != null) {
            boolean sent = connectService.sendPaymentRequest(user.getId(), contact, amount);
            if (sent) {
                statusLabel.setText(String.format(Locale.getDefault(), "Payment request for ₹%.2f sent to %s!", amount, contact));
                statusLabel.setStyle("-fx-text-fill: #4CAF50;");
                ScreenManager.getInstance().showHome();
            } else {
                statusLabel.setText("Failed to record payment request.");
                statusLabel.setStyle("-fx-text-fill: #C10C4E;");
            }
        }
    }
}
