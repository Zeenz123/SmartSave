package com.example.myfirstapp.ui;

import com.example.myfirstapp.model.User;
import com.example.myfirstapp.model.Wallet;
import com.example.myfirstapp.service.AuthService;
import com.example.myfirstapp.service.ReleaseCoinService;
import com.example.myfirstapp.service.WalletService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Recreates ReferActivity.java & activity_refer.xml in JavaFX.
 */
public class ReferView extends VBox {
    private final AuthService authService = AuthService.getInstance();
    private final ReleaseCoinService releaseCoinService = new ReleaseCoinService();
    private final Label referralCodeLabel;
    private final TextField enterFriendCodeField;
    private final Label statusLabel;

    public ReferView() {
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

        Label title = new Label("Refer & Earn");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #263238;");

        topBar.getChildren().addAll(backBtn, title);

        // Center Card
        VBox card = new VBox(14);
        card.getStyleClass().add("white-card");
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(26));

        VBox iconBox = new VBox(IconHelper.getReferIcon(Color.web("#00BCD4"), 2.2));
        iconBox.setAlignment(Pos.CENTER);

        Label cardTitle = new Label("Invite Friends to SaveSmart");
        cardTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #263238;");

        Label cardDesc = new Label("Share your referral code. Earn +25 Release Coins for each successful friend referral!");
        cardDesc.setStyle("-fx-font-size: 14px; -fx-text-fill: #78909C; -fx-text-alignment: center; -fx-wrap-text: true;");

        User user = authService.getCurrentUser();
        Wallet wallet = user != null ? new WalletService().getWallet(user.getId()) : new Wallet(0, 0, 0, 0, 0);
        Label withdrawHint = new Label(wallet.getWithdrawableHint());
        withdrawHint.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #00838F; -fx-text-alignment: center; -fx-wrap-text: true;");

        String code = user != null && user.getReferralCode() != null ? user.getReferralCode() : "SAVE-SMART-789";

        referralCodeLabel = new Label(code);
        referralCodeLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #00BCD4; -fx-background-color: #E0F7FA; -fx-background-radius: 14px; -fx-padding: 10px 28px;");

        Button shareBtn = new Button("Copy & Share Code (+25 Coins)");
        shareBtn.getStyleClass().add("cyan-button");
        shareBtn.setPrefWidth(Double.MAX_VALUE);
        shareBtn.setOnAction(e -> handleShareCode());

        // Redeem a friend's code
        Label enterTitle = new Label("Have a Friend's Code?");
        enterTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #37474F;");

        HBox enterRow = new HBox(8);
        enterFriendCodeField = new TextField();
        enterFriendCodeField.setPromptText("Enter referral code (e.g. SARAH-SAVE-101)");
        enterFriendCodeField.getStyleClass().add("text-input-field");
        HBox.setHgrow(enterFriendCodeField, javafx.scene.layout.Priority.ALWAYS);

        Button applyBtn = new Button("Apply");
        applyBtn.setStyle("-fx-background-color: #00BCD4; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10px;");
        applyBtn.setOnAction(e -> handleApplyFriendCode());

        enterRow.getChildren().addAll(enterFriendCodeField, applyBtn);

        statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        card.getChildren().addAll(iconBox, cardTitle, cardDesc, withdrawHint, referralCodeLabel, shareBtn, enterTitle, enterRow, statusLabel);

        getChildren().addAll(topBar, card);
    }

    private void handleShareCode() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString("Join SaveSmart using my referral code " + referralCodeLabel.getText() + " and start saving for goodness!");
        clipboard.setContent(content);

        statusLabel.setText("Referral code copied to clipboard!");
        statusLabel.setStyle("-fx-text-fill: #4CAF50;");
    }

    private void handleApplyFriendCode() {
        String input = enterFriendCodeField.getText().trim();
        if (input.isEmpty()) {
            statusLabel.setText("Please enter a referral code!");
            statusLabel.setStyle("-fx-text-fill: #C10C4E;");
            return;
        }

        User user = authService.getCurrentUser();
        if (user != null) {
            boolean ok = releaseCoinService.awardReferralCoins(user.getId(), input);
            if (ok) {
                statusLabel.setText("Referral verified! Earned +25 Release Coins!");
                statusLabel.setStyle("-fx-text-fill: #4CAF50;");
                enterFriendCodeField.clear();
            } else {
                statusLabel.setText("Invalid code or already redeemed!");
                statusLabel.setStyle("-fx-text-fill: #C10C4E;");
            }
        }
    }
}
