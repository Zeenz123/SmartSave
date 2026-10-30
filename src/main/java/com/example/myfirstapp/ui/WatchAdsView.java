package com.example.myfirstapp.ui;

import com.example.myfirstapp.model.User;
import com.example.myfirstapp.model.Wallet;
import com.example.myfirstapp.service.AuthService;
import com.example.myfirstapp.service.ReleaseCoinService;
import com.example.myfirstapp.service.WalletService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.UUID;

/**
 * Recreates WatchAdsActivity.java & activity_watch_ads.xml in JavaFX.
 * Awards up to 5 Release Coins with real timer and duplicate prevention.
 */
public class WatchAdsView extends VBox {
    private final AuthService authService = AuthService.getInstance();
    private final ReleaseCoinService releaseCoinService = new ReleaseCoinService();

    private final Button watchAdBtn;
    private final ProgressBar adProgressBar;
    private final Label statusLabel;
    private final String currentAdToken = "AD-SPONSOR-" + (100 + (int)(Math.random() * 900));

    public WatchAdsView() {
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

        Label title = new Label("Watch Ads & Earn");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #263238;");

        topBar.getChildren().addAll(backBtn, title);

        // Center Card
        VBox card = new VBox(14);
        card.getStyleClass().add("white-card");
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(28));

        VBox iconBox = new VBox(IconHelper.getWatchAdsIcon(Color.web("#00BCD4"), 2.2));
        iconBox.setAlignment(Pos.CENTER);

        Label cardTitle = new Label("Watch Short Video Ads");
        cardTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #263238;");

        Label cardDesc = new Label("Earn up to 5 Release Coins for every verified ad completed!");
        cardDesc.setStyle("-fx-font-size: 14px; -fx-text-fill: #78909C; -fx-text-alignment: center; -fx-wrap-text: true;");

        User user = authService.getCurrentUser();
        Wallet wallet = user != null ? new WalletService().getWallet(user.getId()) : new Wallet(0, 0, 0, 0, 0);
        Label withdrawHint = new Label(wallet.getWithdrawableHint());
        withdrawHint.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #00838F; -fx-text-alignment: center; -fx-wrap-text: true;");

        adProgressBar = new ProgressBar(0);
        adProgressBar.setPrefWidth(260);
        adProgressBar.setStyle("-fx-accent: #00BCD4;");
        adProgressBar.setVisible(false);

        watchAdBtn = new Button("Watch Video Ad (+5 Coins)");
        watchAdBtn.getStyleClass().add("cyan-button");
        watchAdBtn.setPrefWidth(Double.MAX_VALUE);
        watchAdBtn.setPrefHeight(52);
        watchAdBtn.setOnAction(e -> startAdPlayback());

        statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        card.getChildren().addAll(iconBox, cardTitle, cardDesc, withdrawHint, adProgressBar, watchAdBtn, statusLabel);

        getChildren().addAll(topBar, card);
    }

    private void startAdPlayback() {
        watchAdBtn.setDisable(true);
        adProgressBar.setVisible(true);
        adProgressBar.setProgress(0);
        statusLabel.setText("Playing sponsored video ad (5s)...");
        statusLabel.setStyle("-fx-text-fill: #00BCD4;");

        Timeline timeline = new Timeline();
        final int[] seconds = {0};
        KeyFrame frame = new KeyFrame(Duration.seconds(1), event -> {
            seconds[0]++;
            adProgressBar.setProgress((double) seconds[0] / 5.0);
            if (seconds[0] >= 5) {
                timeline.stop();
                completeAdReward();
            }
        });
        timeline.getKeyFrames().add(frame);
        timeline.setCycleCount(5);
        timeline.play();
    }

    private void completeAdReward() {
        User user = authService.getCurrentUser();
        if (user != null) {
            boolean awarded = releaseCoinService.awardAdCoins(user.getId(), currentAdToken, 5.0);
            if (awarded) {
                statusLabel.setText("Ad Completed! Earned +5 Release Coins!");
                statusLabel.setStyle("-fx-text-fill: #4CAF50;");
                watchAdBtn.setText("Ad Completed Today");
            } else {
                statusLabel.setText("You have already earned coins for this ad!");
                statusLabel.setStyle("-fx-text-fill: #FF9800;");
            }
        }
    }
}
