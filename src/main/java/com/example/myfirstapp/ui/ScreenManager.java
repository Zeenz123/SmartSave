package com.example.myfirstapp.ui;

import com.example.myfirstapp.model.User;
import com.example.myfirstapp.service.AuthService;
import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Stack;

public class ScreenManager {
    private static ScreenManager instance;
    private Stage stage;
    private StackPane rootContainer;
    private StackPane contentArea;
    private final Stack<Node> historyStack = new Stack<>();

    private ScreenManager() {
    }

    public static synchronized ScreenManager getInstance() {
        if (instance == null) {
            instance = new ScreenManager();
        }
        return instance;
    }

    public void init(Stage stage) {
        this.stage = stage;

        this.contentArea = new StackPane();
        this.contentArea.getStyleClass().add("phone-frame");

        // Outer centered viewport container
        this.rootContainer = new StackPane(contentArea);
        this.rootContainer.setStyle("-fx-background-color: #CFD8DC; -fx-padding: 20px;");

        Scene scene = new Scene(rootContainer, 470, 890);
        scene.getStylesheets().add(getClass().getResource("/com/example/myfirstapp/css/smartsave.css").toExternalForm());

        stage.setTitle("SmartSave — Smart Saving Application");
        stage.setScene(scene);
        stage.setMinWidth(460);
        stage.setMinHeight(880);

        // Initial launch screen matching Android launcher (MainActivity -> LoginView)
        showLogin();
    }

    public void setScreen(Node newView, boolean recordHistory) {
        if (recordHistory && !contentArea.getChildren().isEmpty()) {
            historyStack.push(contentArea.getChildren().get(0));
        }

        contentArea.getChildren().clear();
        contentArea.getChildren().add(newView);

        FadeTransition ft = new FadeTransition(Duration.millis(180), newView);
        ft.setFromValue(0.7);
        ft.setToValue(1.0);
        ft.play();
    }

    public void goBack() {
        if (!historyStack.isEmpty()) {
            Node previous = historyStack.pop();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(previous);

            FadeTransition ft = new FadeTransition(Duration.millis(150), previous);
            ft.setFromValue(0.8);
            ft.setToValue(1.0);
            ft.play();
        } else {
            showHome();
        }
    }

    public void showLogin() {
        historyStack.clear();
        setScreen(new LoginView(), false);
    }

    public void showSignUp() {
        historyStack.clear();
        setScreen(new SignUpView(), false);
    }

    public void showHome() {
        historyStack.clear();
        setScreen(new HomeView(), false);
    }

    public void showAddCash() {
        setScreen(new AddCashView(), true);
    }

    public void showPay() {
        setScreen(new PayView(), true);
    }

    public void showRequestPayment() {
        setScreen(new RequestPaymentView(), true);
    }

    public void showGuardian() {
        setScreen(new GuardianView(), true);
    }

    public void showWithdrawal() {
        setScreen(new WithdrawalView(), true);
    }

    public void showConnect() {
        setScreen(new ConnectView(), true);
    }

    public void showEarnRelease() {
        setScreen(new EarnReleaseView(), true);
    }

    public void showWatchAds() {
        setScreen(new WatchAdsView(), true);
    }

    public void showRefer() {
        setScreen(new ReferView(), true);
    }

    public void showStudyTask() {
        setScreen(new StudyTaskView(), true);
    }

    public void openAiAssistant() {
        AiAssistantDialog dialog = new AiAssistantDialog();
        dialog.show();
    }

    public Stage getStage() {
        return stage;
    }
}
