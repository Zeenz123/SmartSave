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
        flipshowCard.getStyleClass().add("white-card");
        flipshowCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 18px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 12, 0.1, 0, 3);");

        // Slide 1: Hidden state
        slide1Hidden = createSlide1Hidden();

        // Slide 2: Revealed state
        slide2Revealed = createSlide2Revealed();
        slide2Revealed.setVisible(false);

        flipshowCard.getChildren().addAll(slide1Hidden, slide2Revealed);
        flipshowCard.setOnMouseClicked(e -> toggleBalanceFlip());

        // 3. Grid Container: Blue holder outer + white inner holder with 3x3 ImageButtons
        StackPane gridContainer = new StackPane();
        gridContainer.setPrefWidth(365);
        gridContainer.setPrefHeight(375);

        // Cyan background border card
        VBox blueHolder = new VBox();
        blueHolder.setStyle("-fx-background-color: #00BCD4; -fx-background-radius: 20px;");
        blueHolder.setPrefWidth(365);
        blueHolder.setPrefHeight(375);

        // Inner white holder card
        VBox innerHolder = new VBox();
        innerHolder.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 18px;");
        innerHolder.setPadding(new Insets(16, 12, 16, 12));
        innerHolder.setAlignment(Pos.CENTER);
        StackPane.setMargin(innerHolder, new Insets(6, 6, 6, 6));

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(16);
        grid.setVgap(14);

        // Row 1 - Exact order & White icons
        grid.add(createActionButton(IconHelper.getAddCashIcon(Color.WHITE, 1.3), "Add Cash", () -> ScreenManager.getInstance().showAddCash()), 0, 0);
        grid.add(createActionButton(IconHelper.getPayIcon(Color.WHITE, 1.3), "Pay", () -> ScreenManager.getInstance().showPay()), 1, 0);
        grid.add(createActionButton(IconHelper.getRequestPaymentIcon(Color.WHITE, 1.3), "Request Pay", () -> ScreenManager.getInstance().showRequestPayment()), 2, 0);

        // Row 2 - Exact order & White icons
        grid.add(createActionButton(IconHelper.getAddGuardianIcon(Color.WHITE, 1.3), "Guardian", () -> ScreenManager.getInstance().showGuardian()), 0, 1);
        grid.add(createActionButton(IconHelper.getWithdrawalIcon(Color.WHITE, 1.3), "Withdrawal", () -> ScreenManager.getInstance().showWithdrawal()), 1, 1);
        grid.add(createActionButton(IconHelper.getConnectIcon(Color.WHITE, 1.3), "Connect", () -> ScreenManager.getInstance().showConnect()), 2, 1);

        // Row 3 - Exact order & White icons
        grid.add(createActionButton(IconHelper.getEarnReleaseIcon(Color.WHITE, 1.3), "Earn / Release", () -> ScreenManager.getInstance().showEarnRelease()), 0, 2);
        grid.add(createActionButton(IconHelper.getWatchAdsIcon(Color.WHITE, 1.3), "Watch Ads", () -> ScreenManager.getInstance().showWatchAds()), 1, 2);
        grid.add(createActionButton(IconHelper.getReferIcon(Color.WHITE, 1.3), "Refer", () -> ScreenManager.getInstance().showRefer()), 2, 2);

        innerHolder.getChildren().add(grid);
        gridContainer.getChildren().addAll(blueHolder, innerHolder);

        // Bottom User Status Bar
        User currentUser = authService.getCurrentUser();
        String userDisplay = currentUser != null ? currentUser.getFullName() : "Alex Hunter";
        String roleSuffix = "";
        if (currentUser != null && "PARENT".equalsIgnoreCase(currentUser.getRole())) {
            roleSuffix = " (Guardian)";
        }
        Label userStatus = new Label("Logged in as: " + userDisplay + roleSuffix);
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

        Label hideTitle = new Label("Hide Balance");
        hideTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #00BCD4;");

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
        topRow2.getChildren().addAll(hideTitle, topSpacer2, cardRefreshBtn2);

        balanceTextView = new Label("₹100.50");
        balanceTextView.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #000000;");

        targetText = new Label("Target: ₹200.00");
        targetText.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #607D8B; -fx-cursor: hand;");
        targetText.setOnMouseClicked(e -> {
            e.consume();
            new TargetManipulatorDialog(this::refreshWalletData).show();
        });

        Label editTargetChip = new Label("🎯 Edit");
        editTargetChip.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #00838F; -fx-background-color: #E0F7FA; -fx-background-radius: 6px; -fx-padding: 1px 6px; -fx-cursor: hand;");
        editTargetChip.setOnMouseClicked(e -> {
            e.consume();
            new TargetManipulatorDialog(this::refreshWalletData).show();
        });

        HBox targetRow = new HBox(6, targetText, editTargetChip);
        targetRow.setAlignment(Pos.CENTER_LEFT);

        availableText = new Label("Available: ₹85.00 (Locked: ₹30.00)");
        availableText.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");

        coinsText = new Label("Coins: 85 Release Coins");
        coinsText.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #FF9800;");

        withdrawableHintText = new Label("💡 ₹85.00 can be withdrawn (unlocked by 85 Release Coins)");
        withdrawableHintText.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #00838F;");
        withdrawableHintText.setWrapText(true);
        withdrawableHintText.setMaxWidth(215);
        withdrawableHintText.setPrefWidth(215);

        textBox.getChildren().addAll(topRow2, balanceTextView, targetRow, availableText, coinsText, withdrawableHintText);
        content.getChildren().addAll(progressGaugeSlide2, textBox);

        VBox box = new VBox(content);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private void toggleBalanceFlip() {
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
