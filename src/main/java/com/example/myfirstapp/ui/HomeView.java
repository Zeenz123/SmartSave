package com.example.myfirstapp.ui;

import com.example.myfirstapp.model.User;
import com.example.myfirstapp.model.Wallet;
import com.example.myfirstapp.service.AiAssistantService;
import com.example.myfirstapp.service.AuthService;
import com.example.myfirstapp.service.WalletService;
import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.Locale;

/**
 * Recreates HomeActivity.java & activity_home_page.xml in JavaFX.
 * Preserves exact layout, Flipshow card, circular progress bar, 3x3 action grid,
 * and small circular floating AI assistant button.
 */
public class HomeView extends StackPane {
    private final AuthService authService = AuthService.getInstance();
    private final WalletService walletService = new WalletService();
    private final AiAssistantService aiAssistantService = new AiAssistantService();

    private boolean isBalanceRevealed = false;
    private final StackPane flipshowCard;
    private final VBox slide1Hidden;
    private final VBox slide2Revealed;

    private CircularProgressIndicator progressGaugeSlide2;
    private Label balanceTextView;
    private Label targetText;
    private Label availableText;
    private Label coinsText;
    private Label withdrawableHintText;

    private VBox chatFlyout;

    public HomeView() {
        setAlignment(Pos.TOP_CENTER);
        setStyle("-fx-background-color: #E0F7FA;");

        VBox mainLayout = new VBox(14);
        mainLayout.setAlignment(Pos.TOP_CENTER);
        mainLayout.setPadding(new Insets(16, 16, 16, 16));

        // 1. Top Header Banner: Cyan rounded pill with inner white pill "SmartSave"
        StackPane bannerOuter = new StackPane();
        bannerOuter.setPrefWidth(350);
        bannerOuter.setPrefHeight(68);
        bannerOuter.setStyle("-fx-background-color: #00BCD4; -fx-background-radius: 18px;");

        Label smartSaveTitle = new Label("SmartSave");
        smartSaveTitle.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-font-style: italic; -fx-text-fill: #00BCD4; -fx-background-color: #FFFFFF; -fx-background-radius: 14px; -fx-padding: 4px 44px;");

        bannerOuter.getChildren().add(smartSaveTitle);

        // 2. Flipshow Card (ViewFlipper equivalent - 360x200 dp)
        flipshowCard = new StackPane();
        flipshowCard.setPrefWidth(360);
        flipshowCard.setPrefHeight(200);
        flipshowCard.setMaxWidth(360);
        flipshowCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 24px; -fx-border-radius: 24px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0, 188, 212, 0.25), 14, 0.1, 0, 4); -fx-cursor: hand;");

        slide1Hidden = createSlide1Hidden();
        slide2Revealed = createSlide2Revealed();
        slide2Revealed.setVisible(false);

        flipshowCard.getChildren().addAll(slide1Hidden, slide2Revealed);
        flipshowCard.setOnMouseClicked(e -> toggleBalanceVisibility());

        // 3. Grid Menu (3x3 Action Matrix)
        GridPane gridContainer = new GridPane();
        gridContainer.setAlignment(Pos.CENTER);
        gridContainer.setHgap(16);
        gridContainer.setVgap(16);
        gridContainer.setPadding(new Insets(10, 0, 10, 0));

        VBox payBtn = createActionButton(IconHelper.getPayIcon(Color.web("#00BCD4"), 1.2), "Pay", () -> ScreenManager.getInstance().showPay());
        VBox addCashBtn = createActionButton(IconHelper.getAddCashIcon(Color.web("#00BCD4"), 1.2), "Add Cash", () -> ScreenManager.getInstance().showAddCash());
        VBox withdrawBtn = createActionButton(IconHelper.getWithdrawalIcon(Color.web("#00BCD4"), 1.2), "Withdrawal", () -> ScreenManager.getInstance().showWithdrawal());
        VBox requestBtn = createActionButton(IconHelper.getRequestPaymentIcon(Color.web("#00BCD4"), 1.2), "Request", () -> ScreenManager.getInstance().showRequestPayment());
        VBox connectBtn = createActionButton(IconHelper.getConnectIcon(Color.web("#00BCD4"), 1.2), "Connect", () -> ScreenManager.getInstance().showConnect());
        VBox earnBtn = createActionButton(IconHelper.getEarnReleaseIcon(Color.web("#00BCD4"), 1.2), "Earn Coins", () -> ScreenManager.getInstance().showEarnRelease());
        VBox guardianBtn = createActionButton(IconHelper.getAddGuardianIcon(Color.web("#00BCD4"), 1.2), "Guardian", () -> ScreenManager.getInstance().showGuardian());
        VBox adsBtn = createActionButton(IconHelper.getWatchAdsIcon(Color.web("#00BCD4"), 1.2), "Watch Ads", () -> ScreenManager.getInstance().showWatchAds());
        VBox referBtn = createActionButton(IconHelper.getReferIcon(Color.web("#00BCD4"), 1.2), "Refer", () -> ScreenManager.getInstance().showRefer());

        gridContainer.add(payBtn, 0, 0);
        gridContainer.add(addCashBtn, 1, 0);
        gridContainer.add(withdrawBtn, 2, 0);

        gridContainer.add(requestBtn, 0, 1);
        gridContainer.add(connectBtn, 1, 1);
        gridContainer.add(earnBtn, 2, 1);

        gridContainer.add(guardianBtn, 0, 2);
        gridContainer.add(adsBtn, 1, 2);
        gridContainer.add(referBtn, 2, 2);

        User currentUser = authService.getCurrentUser();
        String roleText = "Active User";
        if (currentUser != null && "PARENT".equalsIgnoreCase(currentUser.getRole())) {
            roleText = "Guardian Account";
        }
        Label userStatus = new Label("Logged in as: " + (currentUser != null ? currentUser.getFullName() : "Guest") + " (" + roleText + ")");
        userStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #78909C; -fx-font-weight: bold;");

        Button signOutBtn = new Button("Sign Out");
        signOutBtn.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #C62828; -fx-font-weight: bold; -fx-cursor: hand; " +
                "-fx-background-radius: 14px; -fx-border-color: #FFCDD2; -fx-border-radius: 14px; -fx-padding: 5 16; -fx-font-size: 12px;");
        signOutBtn.setOnAction(e -> {
            AuthService.getInstance().logout();
            ScreenManager.getInstance().showLogin();
        });

        mainLayout.getChildren().addAll(bannerOuter, flipshowCard, gridContainer, userStatus, signOutBtn);

        ScrollPane scrollPane = new ScrollPane(mainLayout);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        // 4. Small Floating Circular AI Button (Bottom Right - 56x56 px circle)
        StackPane aiCircleFab = new StackPane();
        aiCircleFab.setMinSize(56, 56);
        aiCircleFab.setPrefSize(56, 56);
        aiCircleFab.setMaxSize(56, 56);
        aiCircleFab.setStyle("-fx-background-color: linear-gradient(to bottom right, #00BCD4, #00838F); " +
                "-fx-background-radius: 28px; -fx-border-radius: 28px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0, 131, 143, 0.55), 14, 0.2, 0, 4); -fx-cursor: hand;");

        Label aiSparkle = new Label();
        aiSparkle.setGraphic(IconHelper.getAiSparkleIcon(Color.WHITE, 1.4));
        aiCircleFab.getChildren().add(aiSparkle);

        Tooltip.install(aiCircleFab, new Tooltip("SmartSave AI Assistant (Chat & Speak)"));

        aiCircleFab.setOnMouseEntered(e -> {
            aiCircleFab.setScaleX(1.1);
            aiCircleFab.setScaleY(1.1);
        });
        aiCircleFab.setOnMouseExited(e -> {
            aiCircleFab.setScaleX(1.0);
            aiCircleFab.setScaleY(1.0);
        });
        aiCircleFab.setOnMouseClicked(e -> toggleAiFlyout());

        StackPane.setAlignment(aiCircleFab, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(aiCircleFab, new Insets(0, 24, 24, 0));

        // 5. In-App AI Chat Flyout (Docks right above the small circle)
        chatFlyout = createAiChatFlyout();
        chatFlyout.setVisible(false);
        chatFlyout.setManaged(false);
        StackPane.setAlignment(chatFlyout, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(chatFlyout, new Insets(0, 24, 88, 0));

        getChildren().addAll(scrollPane, chatFlyout, aiCircleFab);

        refreshWalletData();
    }

    private void toggleAiFlyout() {
        if (chatFlyout == null) return;
        boolean currentlyOpen = chatFlyout.isVisible();
        if (currentlyOpen) {
            FadeTransition ft = new FadeTransition(Duration.millis(150), chatFlyout);
            ft.setFromValue(1.0);
            ft.setToValue(0.0);
            ft.setOnFinished(e -> {
                chatFlyout.setVisible(false);
                chatFlyout.setManaged(false);
            });
            ft.play();
        } else {
            chatFlyout.setVisible(true);
            chatFlyout.setManaged(true);
            FadeTransition ft = new FadeTransition(Duration.millis(200), chatFlyout);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
        }
    }

    private VBox createAiChatFlyout() {
        VBox flyout = new VBox(8);
        flyout.setPrefWidth(340);
        flyout.setMaxWidth(340);
        flyout.setPrefHeight(430);
        flyout.setMaxHeight(430);
        flyout.setPadding(new Insets(12));
        flyout.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 18px; -fx-border-color: #00BCD4; -fx-border-width: 1.5px; " +
                "-fx-border-radius: 18px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 18, 0.1, 0, 4);");

        // Top Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 6, 0));
        header.setStyle("-fx-border-color: #E0F2F1; -fx-border-width: 0 0 1 0;");

        Label icon = new Label();
        icon.setGraphic(IconHelper.getAiSparkleIcon(Color.web("#00838F"), 1.2));

        VBox titleBox = new VBox(1);
        Label title = new Label("SmartSave AI Coach");
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #00838F;");
        Label subtitle = new Label("Gemini 2.5 Flash • Voice & Chat");
        subtitle.setStyle("-fx-font-size: 10px; -fx-text-fill: #78909C;");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #78909C; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 0 4;");
        closeBtn.setOnAction(e -> toggleAiFlyout());

        header.getChildren().addAll(icon, titleBox, spacer, closeBtn);

        // Chat Message History Area
        TextArea chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-background-color: #F8FDFF; -fx-background-radius: 10px; -fx-border-color: #E0F2F1; -fx-border-radius: 10px;");
        VBox.setVgrow(chatArea, Priority.ALWAYS);

        User user = authService.getCurrentUser();
        chatArea.setText(aiAssistantService.getPersonalizedGreeting(user));

        // Quick Suggestion Chips
        HBox chips = new HBox(6);
        chips.setAlignment(Pos.CENTER_LEFT);

        Button c1 = createFlyoutChip("📊 Spending", chatArea, "Analyze my recent spending");
        Button c2 = createFlyoutChip("🎯 Forecast", chatArea, "Predict my target savings date");
        Button c3 = createFlyoutChip("💡 Advice", chatArea, "Tips to avoid impulse purchases");
        chips.getChildren().addAll(c1, c2, c3);

        // Input & Controls Row
        HBox inputRow = new HBox(6);
        inputRow.setAlignment(Pos.CENTER);

        TextField promptField = new TextField();
        promptField.setPromptText("Ask AI coach...");
        promptField.setStyle("-fx-font-size: 12px; -fx-background-radius: 12px; -fx-border-color: #CFD8DC; -fx-border-radius: 12px; -fx-padding: 6 10;");
        HBox.setHgrow(promptField, Priority.ALWAYS);

        Button sendBtn = new Button("➤");
        sendBtn.setStyle("-fx-background-color: #00BCD4; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 12px; -fx-cursor: hand; -fx-padding: 6 12;");

        Button speakBtn = new Button("🔊");
        speakBtn.setStyle("-fx-background-color: #00838F; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 12px; -fx-cursor: hand; -fx-padding: 6 10;");

        Runnable executeSend = () -> {
            String q = promptField.getText().trim();
            if (q.isEmpty()) return;
            chatArea.appendText("\n\n🧑 You: " + q);
            promptField.clear();
            String ans = aiAssistantService.answerQuestion(q, authService.getCurrentUser());
            chatArea.appendText("\n\n" + ans);
            speakText(ans);
        };

        sendBtn.setOnAction(e -> executeSend.run());
        promptField.setOnAction(e -> executeSend.run());
        speakBtn.setOnAction(e -> {
            String currentText = chatArea.getText();
            if (!currentText.isEmpty()) {
                int lastAiIdx = currentText.lastIndexOf("🤖");
                if (lastAiIdx != -1) {
                    speakText(currentText.substring(lastAiIdx));
                } else {
                    speakText(currentText);
                }
            }
        });

        inputRow.getChildren().addAll(promptField, sendBtn, speakBtn);

        flyout.getChildren().addAll(header, chatArea, chips, inputRow);
        return flyout;
    }

    private Button createFlyoutChip(String label, TextArea chatArea, String query) {
        Button chip = new Button(label);
        chip.setStyle("-fx-background-color: #E0F7FA; -fx-text-fill: #00838F; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 10px; -fx-cursor: hand; -fx-padding: 4 8;");
        chip.setOnAction(e -> {
            chatArea.appendText("\n\n🧑 You: " + query);
            String ans = aiAssistantService.answerQuestion(query, authService.getCurrentUser());
            chatArea.appendText("\n\n" + ans);
            speakText(ans);
        });
        return chip;
    }

    private void speakText(String text) {
        if (text == null || text.isBlank()) return;
        Thread t = new Thread(() -> {
            try {
                String clean = text.replaceAll("[#*✨📊🎯💡🎬📺•👋🧑🤖\r\n\"]", " ").trim();
                if (clean.length() > 250) clean = clean.substring(0, 250);
                ProcessBuilder pb = new ProcessBuilder("powershell", "-Command",
                        "Add-Type -AssemblyName System.Speech; $s = New-Object System.Speech.Synthesis.SpeechSynthesizer; $s.Speak('" + clean.replace("'", "") + "')");
                pb.start();
            } catch (Exception ignored) {}
        });
        t.setDaemon(true);
        t.start();
    }

    private VBox createSlide1Hidden() {
        HBox content = new HBox(16);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(14));

        CircularProgressIndicator gauge = new CircularProgressIndicator(40, 0, "***");

        VBox textBox = new VBox(4);
        textBox.setAlignment(Pos.CENTER_LEFT);

        HBox topRow1 = new HBox(8);
        topRow1.setAlignment(Pos.CENTER_LEFT);
        topRow1.setPrefWidth(215);

        Label showTitle = new Label("Show Balance");
        showTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #00BCD4;");

        Region topSpacer1 = new Region();
        HBox.setHgrow(topSpacer1, Priority.ALWAYS);

        Button cardRefreshBtn1 = new Button();
        cardRefreshBtn1.setGraphic(IconHelper.getRefreshIcon(Color.web("#00BCD4"), 0.85));
        cardRefreshBtn1.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16px; -fx-border-color: #B2EBF2; -fx-border-radius: 16px; " +
                "-fx-min-width: 32px; -fx-min-height: 32px; -fx-max-width: 32px; -fx-max-height: 32px; -fx-cursor: hand; -fx-padding: 0; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0.05, 0, 1);");
        cardRefreshBtn1.setOnAction(e -> {
            e.consume();
            RotateTransition rt = new RotateTransition(Duration.millis(400), cardRefreshBtn1.getGraphic());
            rt.setByAngle(360);
            rt.play();
            refreshWalletData();
        });
        topRow1.getChildren().addAll(showTitle, topSpacer1, cardRefreshBtn1);

        Label hiddenAmount = new Label("₹****");
        hiddenAmount.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #000000;");

        Label hint = new Label("Tap to view details");
        hint.setStyle("-fx-font-size: 13px; -fx-text-fill: #78909C;");

        textBox.getChildren().addAll(topRow1, hiddenAmount, hint);
        content.getChildren().addAll(gauge, textBox);

        VBox box = new VBox(content);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private VBox createSlide2Revealed() {
        HBox content = new HBox(16);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(14));

        progressGaugeSlide2 = new CircularProgressIndicator(40, 50, "50%");

        VBox textBox = new VBox(2);
        textBox.setAlignment(Pos.CENTER_LEFT);

        HBox topRow2 = new HBox(8);
        topRow2.setAlignment(Pos.CENTER_LEFT);
        topRow2.setPrefWidth(215);

        Label totalLabel = new Label("Total Balance");
        totalLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #78909C; -fx-font-weight: bold;");

        Region topSpacer2 = new Region();
        HBox.setHgrow(topSpacer2, Priority.ALWAYS);

        Button cardRefreshBtn2 = new Button();
        cardRefreshBtn2.setGraphic(IconHelper.getRefreshIcon(Color.web("#00BCD4"), 0.85));
        cardRefreshBtn2.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16px; -fx-border-color: #B2EBF2; -fx-border-radius: 16px; " +
                "-fx-min-width: 32px; -fx-min-height: 32px; -fx-max-width: 32px; -fx-max-height: 32px; -fx-cursor: hand; -fx-padding: 0; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0.05, 0, 1);");
        cardRefreshBtn2.setOnAction(e -> {
            e.consume();
            RotateTransition rt = new RotateTransition(Duration.millis(400), cardRefreshBtn2.getGraphic());
            rt.setByAngle(360);
            rt.play();
            refreshWalletData();
        });
        topRow2.getChildren().addAll(totalLabel, topSpacer2, cardRefreshBtn2);

        balanceTextView = new Label("₹100.50");
        balanceTextView.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #000000;");

        targetText = new Label("Target: ₹200.00");
        targetText.setStyle("-fx-font-size: 12px; -fx-text-fill: #546E7A;");

        availableText = new Label("Available: ₹60.50 (Locked: ₹40.00)");
        availableText.setStyle("-fx-font-size: 12px; -fx-text-fill: #00BCD4; -fx-font-weight: bold;");

        coinsText = new Label("Coins: 65 Release Coins");
        coinsText.setStyle("-fx-font-size: 11px; -fx-text-fill: #FF9800; -fx-font-weight: bold;");

        withdrawableHintText = new Label("💡 ₹65.00 can be withdrawn (unlocked by 65 Release Coins)");
        withdrawableHintText.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #00838F;");

        textBox.getChildren().addAll(topRow2, balanceTextView, targetText, availableText, coinsText, withdrawableHintText);
        content.getChildren().addAll(progressGaugeSlide2, textBox);

        VBox box = new VBox(content);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private void toggleBalanceVisibility() {
        isBalanceRevealed = !isBalanceRevealed;
        if (isBalanceRevealed) {
            refreshWalletData();
            slide1Hidden.setVisible(false);
            slide2Revealed.setVisible(true);
            FadeTransition ft = new FadeTransition(Duration.millis(200), slide2Revealed);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        } else {
            slide2Revealed.setVisible(false);
            slide1Hidden.setVisible(true);
            FadeTransition ft = new FadeTransition(Duration.millis(200), slide1Hidden);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        }
    }

    public void refreshWalletData() {
        User user = authService.getCurrentUser();
        if (user == null) return;

        Wallet wallet = walletService.getWallet(user.getId());
        double total = wallet.getTotalBalance();
        double effectiveAvailable = wallet.getEffectiveBalance();
        double effectiveLocked = wallet.getEffectiveLockedSavings();
        double target = wallet.getTargetBalance();
        double coins = wallet.getReleaseCoins();
        int percentage = wallet.getProgressPercentage();

        if (progressGaugeSlide2 != null) {
            progressGaugeSlide2.setProgress(percentage, percentage + "%");
        }
        if (balanceTextView != null) {
            balanceTextView.setText(String.format(Locale.getDefault(), "₹%.2f", total));
        }
        if (targetText != null) {
            targetText.setText(String.format(Locale.getDefault(), "Target: ₹%.2f", target));
        }
        if (availableText != null) {
            availableText.setText(String.format(Locale.getDefault(), "Available: ₹%.2f (Locked: ₹%.2f)", effectiveAvailable, effectiveLocked));
        }
        if (coinsText != null) {
            coinsText.setText(String.format(Locale.getDefault(), "Coins: %.0f Release Coins", coins));
        }
        if (withdrawableHintText != null) {
            withdrawableHintText.setText(wallet.getWithdrawableHint());
        }
    }

    private VBox createActionButton(Node icon, String labelText, Runnable onClick) {
        StackPane btn = new StackPane(icon);
        btn.getStyleClass().add("grid-action-button");
        btn.setOnMouseClicked(e -> onClick.run());

        Label label = new Label(labelText);
        label.getStyleClass().add("grid-action-label");

        VBox box = new VBox(6, btn, label);
        box.setAlignment(Pos.CENTER);
        return box;
    }
}
