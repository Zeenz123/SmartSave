package com.example.myfirstapp.ui;

import com.example.myfirstapp.algo.Trie;
import com.example.myfirstapp.dao.UserDao;
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
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Recreates PayActivity.java & activity_pay.xml in JavaFX.
 * Implements genuine peer-to-peer virtual transfers between registered users.
 * Displays live Payment & Transfer History directly beneath the payment form.
 */
public class PayView extends VBox {
    private final AuthService authService = AuthService.getInstance();
    private final WalletService walletService = new WalletService();

    private final TextField recipientField;
    private final TextField amountField;
    private final Label balanceHintLabel;
    private final Label statusLabel;
    private final HBox statusBanner;
    private final VBox historyItemsContainer;

    public PayView() {
        setSpacing(12);
        setPadding(new Insets(14, 16, 20, 16));
        setStyle("-fx-background-color: #E0F7FA;");

        User me = authService.getCurrentUser();

        // 1. Top Navigation Bar
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button();
        backBtn.setGraphic(IconHelper.getBackArrowIcon(Color.web("#263238"), 1.2));
        backBtn.getStyleClass().add("back-button");
        backBtn.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20px; -fx-padding: 8px; -fx-cursor: hand;");
        backBtn.setOnAction(e -> ScreenManager.getInstance().goBack());

        Label title = new Label("Pay / Send Money");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #263238;");

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        Button refreshBtn = new Button();
        refreshBtn.setGraphic(IconHelper.getRefreshIcon(Color.web("#00BCD4"), 0.95));
        refreshBtn.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20px; -fx-min-width: 36px; -fx-min-height: 36px; " +
                "-fx-max-width: 36px; -fx-max-height: 36px; -fx-padding: 0; -fx-cursor: hand;");
        refreshBtn.setOnAction(e -> {
            refreshBalance();
            refreshHistory();
        });

        topBar.getChildren().addAll(backBtn, title, topSpacer, refreshBtn);

        // 2. Status / Feedback Banner
        statusBanner = new HBox(10);
        statusBanner.setAlignment(Pos.CENTER_LEFT);
        statusBanner.setPadding(new Insets(10, 16, 10, 16));
        statusBanner.setVisible(false);
        statusBanner.setManaged(false);

        statusLabel = new Label("");
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        statusBanner.getChildren().add(statusLabel);

        // 3. Payment Form Card
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20px; -fx-border-radius: 20px; " +
                "-fx-border-color: #B2EBF2; -fx-padding: 18px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0.05, 0, 3);");

        Label recipientHeader = new Label("Recipient Details");
        recipientHeader.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #00BCD4;");

        recipientField = new TextField();
        recipientField.setPromptText("Username / Email / Phone (e.g. sarah)");
        recipientField.setStyle("-fx-font-size: 14px; -fx-text-fill: #263238; -fx-background-color: #F5F7F8; -fx-border-color: #CFD8DC; " +
                "-fx-border-radius: 14px; -fx-background-radius: 14px; -fx-padding: 10px 14px;");

        if (me != null) {
            Trie userTrie = new Trie();
            for (User u : new UserDao().findAllExcept(me.getId())) {
                userTrie.insert(u.getUsername());
                userTrie.insert(u.getFullName());
            }
            AutoComplete.attach(recipientField, userTrie);
        }

        Label amountHeader = new Label("Amount (₹)");
        amountHeader.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #00BCD4;");

        amountField = new TextField();
        amountField.setPromptText("0.00");
        amountField.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #263238; -fx-background-color: #F5F7F8; " +
                "-fx-background-radius: 14px; -fx-border-color: #CFD8DC; -fx-border-radius: 14px; -fx-padding: 10px 14px;");

        balanceHintLabel = new Label();
        balanceHintLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #00838F; -fx-font-weight: bold; -fx-wrap-text: true;");
        refreshBalance();

        Button sendBtn = new Button("Send Payment Now");
        sendBtn.getStyleClass().add("cyan-button");
        sendBtn.setStyle("-fx-background-color: #00BCD4; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; " +
                "-fx-background-radius: 16px; -fx-padding: 12px 24px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0, 188, 212, 0.3), 8, 0.1, 0, 3);");
        sendBtn.setPrefWidth(Double.MAX_VALUE);
        sendBtn.setOnAction(e -> handlePayment());

        card.getChildren().addAll(recipientHeader, recipientField, amountHeader, amountField, balanceHintLabel, sendBtn);

        // 4. Payment History Card
        VBox historyCard = new VBox(10);
        historyCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20px; -fx-border-radius: 20px; " +
                "-fx-border-color: #B2EBF2; -fx-padding: 18px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0.05, 0, 3);");

        HBox historyHeader = new HBox(8);
        historyHeader.setAlignment(Pos.CENTER_LEFT);

        Label historyTitle = new Label("Payment & Transfer History");
        historyTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #00838F;");

        Region histSpacer = new Region();
        HBox.setHgrow(histSpacer, Priority.ALWAYS);

        Button histRefresh = new Button();
        histRefresh.setGraphic(IconHelper.getRefreshIcon(Color.web("#00BCD4"), 0.8));
        histRefresh.setStyle("-fx-background-color: #F5F7F8; -fx-background-radius: 14px; -fx-border-color: #CFD8DC; " +
                "-fx-border-radius: 14px; -fx-cursor: hand; -fx-min-width: 28px; -fx-min-height: 28px; -fx-padding: 0;");
        histRefresh.setOnAction(e -> refreshHistory());

        historyHeader.getChildren().addAll(historyTitle, histSpacer, histRefresh);

        historyItemsContainer = new VBox(8);
        historyCard.getChildren().addAll(historyHeader, historyItemsContainer);

        // Content Box & ScrollPane
        VBox contentBox = new VBox(16);
        contentBox.getChildren().addAll(card, historyCard);

        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-hbar-policy: never;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(topBar, statusBanner, scrollPane);

        refreshHistory();
    }

    private void handlePayment() {
        String recipientInput = recipientField.getText().trim();
        String amountInput = amountField.getText().trim();

        if (recipientInput.isEmpty()) {
            showToast("Please enter recipient details!", false);
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountInput);
            if (amount <= 0.0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showToast("Please enter a valid positive amount!", false);
            return;
        }

        User user = authService.getCurrentUser();
        if (user == null) {
            showToast("Session expired. Please log in again.", false);
            return;
        }

        WalletService.TransferResult result = walletService.transferFunds(
                user.getId(), recipientInput, amount, "Virtual P2P Payment");

        if (result.isSuccess()) {
            showToast(result.getMessage(), true);
            recipientField.clear();
            amountField.clear();
            refreshBalance();
            refreshHistory();
        } else {
            showToast(result.getMessage(), false);
        }
    }

    private void showToast(String message, boolean isSuccess) {
        statusLabel.setText(message);
        if (isSuccess) {
            statusBanner.setStyle("-fx-background-color: #E8F5E9; -fx-background-radius: 16px; -fx-border-color: #A5D6A7; -fx-border-radius: 16px;");
            statusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");
        } else {
            statusBanner.setStyle("-fx-background-color: #FFEBEE; -fx-background-radius: 16px; -fx-border-color: #FFCDD2; -fx-border-radius: 16px;");
            statusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #C62828;");
        }
        statusBanner.setVisible(true);
        statusBanner.setManaged(true);
    }

    public void refreshBalance() {
        User user = authService.getCurrentUser();
        Wallet wallet = user != null ? walletService.getWallet(user.getId()) : new Wallet(0, 0, 0, 0, 0);
        balanceHintLabel.setText(wallet.getWithdrawableHint());
    }

    public void refreshHistory() {
        User user = authService.getCurrentUser();
        if (user != null && historyItemsContainer != null) {
            PaymentHistoryHelper.populateHistoryBox(historyItemsContainer, user.getId(), 15);
        }
    }
}
