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
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.Locale;

/**
 * Recreates EarnReleaseActivity.java & activity_earn_release.xml in JavaFX.
 */
public class EarnReleaseView extends VBox {
    private final AuthService authService = AuthService.getInstance();
    private final ReleaseCoinService releaseCoinService = new ReleaseCoinService();
    private final WalletService walletService = new WalletService();
    private final Label coinsValueText;
    private final Label withdrawableHintText;

    public EarnReleaseView() {
        setSpacing(16);
        setPadding(new Insets(24, 20, 24, 20));
        setStyle("-fx-background-color: #E0F7FA;");

        // Top Navigation Bar
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button();
        backBtn.setGraphic(IconHelper.getBackArrowIcon(Color.web("#263238"), 1.2));
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> ScreenManager.getInstance().goBack());

        Label title = new Label("Earn Release Coins");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #263238;");

        topBar.getChildren().addAll(backBtn, title);

        VBox contentBox = new VBox(14);

        // Coins Header Card
        VBox coinsCard = new VBox(4);
        coinsCard.getStyleClass().add("white-card");
        coinsCard.setAlignment(Pos.CENTER);
        coinsCard.setPadding(new Insets(20));

        coinsValueText = new Label("50 Coins");
        coinsValueText.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #00BCD4;");

        Label subtitle = new Label("Your Release Coins Balance (1 Coin = ₹1.00)");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #78909C;");

        withdrawableHintText = new Label();
        withdrawableHintText.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #00838F;");

        coinsCard.getChildren().addAll(coinsValueText, subtitle, withdrawableHintText);

        // Section Title
        Label sectionTitle = new Label("Ways to Earn Coins & Early Release");
        sectionTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #263238;");

        // AI Study Task 1 Card
        VBox task1Card = new VBox(6);
        task1Card.getStyleClass().add("white-card");
        Label t1Title = new Label("AI Task 1: Complete Study Video Feed");
        t1Title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #263238;");
        Label t1Desc = new Label("Watch recommended educational video feed (+15 Coins)");
        t1Desc.setStyle("-fx-font-size: 13px; -fx-text-fill: #607D8B;");
        Button t1Btn = new Button("Open Study Module (+15 Coins)");
        t1Btn.setStyle("-fx-background-color: #00BCD4; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-cursor: hand;");
        t1Btn.setOnAction(e -> ScreenManager.getInstance().showStudyTask());
        task1Card.getChildren().addAll(t1Title, t1Desc, t1Btn);

        // AI Study Task 2 Card
        VBox task2Card = new VBox(6);
        task2Card.getStyleClass().add("white-card");
        Label t2Title = new Label("AI Task 2: Solve Daily Subject Quiz");
        t2Title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #263238;");
        Label t2Desc = new Label("Solve subject-related questions with AI verification (+20 Coins)");
        t2Desc.setStyle("-fx-font-size: 13px; -fx-text-fill: #607D8B;");
        Button t2Btn = new Button("Solve Quiz (+20 Coins)");
        t2Btn.setStyle("-fx-background-color: #00BCD4; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-cursor: hand;");
        t2Btn.setOnAction(e -> ScreenManager.getInstance().showStudyTask());
        task2Card.getChildren().addAll(t2Title, t2Desc, t2Btn);

        // Quick Earn Shortcuts Row
        HBox shortcutsRow = new HBox(10);
        Button adsBtn = new Button("Watch Ads (+5 Coins)");
        adsBtn.setStyle("-fx-background-color: #00BCD4; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10px; -fx-pref-height: 46px; -fx-cursor: hand;");
        HBox.setHgrow(adsBtn, Priority.ALWAYS);
        adsBtn.setMaxWidth(Double.MAX_VALUE);
        adsBtn.setOnAction(e -> ScreenManager.getInstance().showWatchAds());

        Button referBtn = new Button("Refer Friends (+25 Coins)");
        referBtn.setStyle("-fx-background-color: #00BCD4; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10px; -fx-pref-height: 46px; -fx-cursor: hand;");
        HBox.setHgrow(referBtn, Priority.ALWAYS);
        referBtn.setMaxWidth(Double.MAX_VALUE);
        referBtn.setOnAction(e -> ScreenManager.getInstance().showRefer());

        shortcutsRow.getChildren().addAll(adsBtn, referBtn);

        contentBox.getChildren().addAll(coinsCard, sectionTitle, task1Card, task2Card, shortcutsRow);

        ScrollPane scroll = new ScrollPane(contentBox);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");

        getChildren().addAll(topBar, scroll);

        refreshCoins();
    }

    private void refreshCoins() {
        User user = authService.getCurrentUser();
        if (user != null) {
            double coins = releaseCoinService.getReleaseCoins(user.getId());
            coinsValueText.setText(String.format(Locale.getDefault(), "%.0f Coins", coins));
            Wallet wallet = walletService.getWallet(user.getId());
            withdrawableHintText.setText(wallet.getWithdrawableHint());
        }
    }
}
