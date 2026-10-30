package com.example.myfirstapp.ui;

import com.example.myfirstapp.model.User;
import com.example.myfirstapp.model.Wallet;
import com.example.myfirstapp.service.AuthService;
import com.example.myfirstapp.service.EarlyReleaseService;
import com.example.myfirstapp.service.EducationalTaskService;
import com.example.myfirstapp.service.WalletService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Educational Study Session View with:
 * - Forward-skipping restricted to 25 seconds
 * - Backward seeking permitted freely
 * - Real-time playback timer
 * - MCQ evaluation
 * - Release Coin reward on completion
 */
public class StudyTaskView extends VBox {
    private final AuthService authService = AuthService.getInstance();
    private final EducationalTaskService taskService = new EducationalTaskService();
    private final EarlyReleaseService earlyReleaseService = new EarlyReleaseService();

    private final ComboBox<String> taskSelector;
    private final Label taskTitleLabel;
    private final Label taskResourceLabel;
    private final ProgressBar sessionProgressBar;
    private final Label timeLabel;
    private final Slider timelineSlider;
    private final Button playPauseBtn;
    private final VBox quizContainer;
    private final Button submitBtn;
    private final Label feedbackLabel;

    private List<EducationalTaskService.TaskTemplate> availableTasks;
    private EducationalTaskService.TaskTemplate selectedTask;
    private EducationalTaskService.StudySession studySession;
    private Timeline playbackTimer;
    private boolean isPlaying = false;
    private final List<ToggleGroup> quizGroups = new ArrayList<>();

    public StudyTaskView() {
        setSpacing(14);
        setPadding(new Insets(20, 18, 20, 18));
        setStyle("-fx-background-color: #E0F7FA;");

        // Top Bar
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button();
        backBtn.setGraphic(IconHelper.getBackArrowIcon(Color.web("#263238"), 1.2));
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> {
            stopPlayback();
            ScreenManager.getInstance().goBack();
        });

        Label title = new Label("Educational Study Session");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #263238;");

        topBar.getChildren().addAll(backBtn, title);

        VBox content = new VBox(14);

        // 1. Task Selection Card
        VBox selectorCard = new VBox(8);
        selectorCard.getStyleClass().add("white-card");
        selectorCard.setPadding(new Insets(14));

        Label selectHeader = new Label("Select Study Module");
        selectHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #00BCD4;");

        taskSelector = new ComboBox<>();
        taskSelector.setMaxWidth(Double.MAX_VALUE);
        taskSelector.setOnAction(e -> loadSelectedTask(taskSelector.getSelectionModel().getSelectedIndex()));

        taskTitleLabel = new Label();
        taskTitleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #263238;");

        taskResourceLabel = new Label();
        taskResourceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #00838F;");

        User user = authService.getCurrentUser();
        Wallet wallet = user != null ? new WalletService().getWallet(user.getId()) : new Wallet(0, 0, 0, 0, 0);
        Label withdrawHint = new Label(wallet.getWithdrawableHint());
        withdrawHint.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #00838F;");

        selectorCard.getChildren().addAll(selectHeader, taskSelector, taskTitleLabel, taskResourceLabel, withdrawHint);

        // 2. Interactive Study Player Card
        VBox playerCard = new VBox(12);
        playerCard.getStyleClass().add("white-card");
        playerCard.setPadding(new Insets(16));

        Label playerHeader = new Label("Simulated Study Lecture Feed");
        playerHeader.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #00BCD4;");

        timeLabel = new Label("00:00 / 01:00");
        timeLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #37474F;");

        sessionProgressBar = new ProgressBar(0);
        sessionProgressBar.setMaxWidth(Double.MAX_VALUE);
        sessionProgressBar.setStyle("-fx-accent: #00BCD4;");

        timelineSlider = new Slider(0, 100, 0);
        timelineSlider.setOnMouseReleased(e -> handleSeek((int) timelineSlider.getValue()));

        Label ruleNotice = new Label("🛡️ Anti-Cheating: Forward skipping restricted to 25s max. Backward review is unlimited.");
        ruleNotice.setStyle("-fx-font-size: 11px; -fx-text-fill: #E65100; -fx-font-style: italic; -fx-wrap-text: true;");

        HBox controls = new HBox(12);
        controls.setAlignment(Pos.CENTER);

        Button rewBtn = new Button("⏪ -15s");
        rewBtn.setStyle("-fx-background-color: #B2EBF2; -fx-text-fill: #006064; -fx-font-weight: bold; -fx-cursor: hand;");
        rewBtn.setOnAction(e -> handleRelativeSeek(-15));

        playPauseBtn = new Button("▶ Start Study Session");
        playPauseBtn.getStyleClass().add("cyan-button");
        playPauseBtn.setOnAction(e -> togglePlayPause());

        Button fwdBtn = new Button("+20s ⏩");
        fwdBtn.setStyle("-fx-background-color: #B2EBF2; -fx-text-fill: #006064; -fx-font-weight: bold; -fx-cursor: hand;");
        fwdBtn.setOnAction(e -> handleRelativeSeek(20));

        controls.getChildren().addAll(rewBtn, playPauseBtn, fwdBtn);

        playerCard.getChildren().addAll(playerHeader, timeLabel, sessionProgressBar, timelineSlider, ruleNotice, controls);

        // 3. Quiz Section Card
        quizContainer = new VBox(10);
        quizContainer.getStyleClass().add("white-card");
        quizContainer.setPadding(new Insets(16));

        submitBtn = new Button("Submit Answers & Claim Release Coins");
        submitBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 12px; -fx-background-radius: 10px; -fx-cursor: hand;");
        submitBtn.setPrefWidth(Double.MAX_VALUE);
        submitBtn.setOnAction(e -> handleSubmitQuiz());

        feedbackLabel = new Label("");
        feedbackLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        content.getChildren().addAll(selectorCard, playerCard, quizContainer, submitBtn, feedbackLabel);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");

        getChildren().addAll(topBar, scroll);

        loadUserTasks();
    }

    private void loadUserTasks() {
        User user = authService.getCurrentUser();
        availableTasks = taskService.getAvailableTasksForUser(user != null ? user.getId() : 0);

        taskSelector.getItems().clear();
        for (EducationalTaskService.TaskTemplate t : availableTasks) {
            taskSelector.getItems().add(t.title + " (+" + (int) t.coinsReward + " Coins)");
        }
        if (!availableTasks.isEmpty()) {
            taskSelector.getSelectionModel().select(0);
            loadSelectedTask(0);
        }
    }

    private void loadSelectedTask(int index) {
        if (index < 0 || index >= availableTasks.size()) return;
        stopPlayback();

        selectedTask = availableTasks.get(index);
        studySession = new EducationalTaskService.StudySession(selectedTask);

        taskTitleLabel.setText(selectedTask.title + " [" + selectedTask.category + "]");
        taskResourceLabel.setText("Resource URL: " + selectedTask.resourceUrl);

        timelineSlider.setMax(selectedTask.durationSeconds);
        timelineSlider.setValue(0);
        updateProgressDisplay();
        renderQuiz();
    }

    private void renderQuiz() {
        quizContainer.getChildren().clear();
        quizGroups.clear();

        Label quizTitle = new Label("Module Comprehension Quiz");
        quizTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #00BCD4;");
        quizContainer.getChildren().add(quizTitle);

        if (selectedTask.questions == null || selectedTask.questions.isEmpty()) {
            quizContainer.getChildren().add(new Label("No quiz required for this module."));
            return;
        }

        int qIndex = 1;
        for (EducationalTaskService.MCQ q : selectedTask.questions) {
            VBox qBox = new VBox(6);
            Label qText = new Label(qIndex + ". " + q.question);
            qText.setStyle("-fx-font-weight: bold; -fx-text-fill: #263238; -fx-wrap-text: true;");

            ToggleGroup group = new ToggleGroup();
            quizGroups.add(group);

            qBox.getChildren().add(qText);
            for (int i = 0; i < q.options.size(); i++) {
                RadioButton rb = new RadioButton(q.options.get(i));
                rb.setToggleGroup(group);
                rb.setUserData(i);
                rb.setStyle("-fx-text-fill: #37474F;");
                qBox.getChildren().add(rb);
            }
            quizContainer.getChildren().add(qBox);
            qIndex++;
        }
    }

    private void togglePlayPause() {
        if (isPlaying) {
            stopPlayback();
        } else {
            startPlayback();
        }
    }

    private void startPlayback() {
        isPlaying = true;
        playPauseBtn.setText("⏸ Pause Session");
        playbackTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            studySession.tickSecond();
            updateProgressDisplay();
            if (studySession.getCurrentSeconds() >= selectedTask.durationSeconds) {
                stopPlayback();
                feedbackLabel.setText("Study session finished! Please solve the quiz below to claim coins.");
                feedbackLabel.setStyle("-fx-text-fill: #00BCD4;");
            }
        }));
        playbackTimer.setCycleCount(Timeline.INDEFINITE);
        playbackTimer.play();
    }

    private void stopPlayback() {
        isPlaying = false;
        playPauseBtn.setText("▶ Resume Study Session");
        if (playbackTimer != null) {
            playbackTimer.stop();
        }
    }

    private void handleSeek(int target) {
        boolean ok = studySession.seekTo(target);
        if (!ok) {
            feedbackLabel.setText("⚠️ Forward skip limited to 25 seconds to protect study discipline.");
            feedbackLabel.setStyle("-fx-text-fill: #E65100;");
        }
        updateProgressDisplay();
    }

    private void handleRelativeSeek(int delta) {
        int target = studySession.getCurrentSeconds() + delta;
        handleSeek(target);
    }

    private void updateProgressDisplay() {
        int cur = studySession.getCurrentSeconds();
        int total = selectedTask.durationSeconds;
        timeLabel.setText(String.format("%02d:%02d / %02d:%02d", cur / 60, cur % 60, total / 60, total % 60));
        sessionProgressBar.setProgress((double) cur / total);
        timelineSlider.setValue(cur);
    }

    private void handleSubmitQuiz() {
        int correctCount = 0;
        for (int i = 0; i < selectedTask.questions.size(); i++) {
            EducationalTaskService.MCQ q = selectedTask.questions.get(i);
            ToggleGroup tg = quizGroups.get(i);
            if (tg.getSelectedToggle() != null) {
                int selectedAnswer = (int) tg.getSelectedToggle().getUserData();
                if (selectedAnswer == q.correctIndex) {
                    correctCount++;
                }
            }
        }

        User user = authService.getCurrentUser();
        if (user == null) return;

        boolean success = taskService.completeTask(user.getId(), selectedTask, studySession.getMaxUnlockedSeconds(), correctCount);
        if (success) {
            earlyReleaseService.grantTaskApproval(user.getId(), selectedTask.title);
            feedbackLabel.setText("🎉 Verified Complete! Earned +" + (int) selectedTask.coinsReward + " Release Coins & Early Release Authority!");
            feedbackLabel.setStyle("-fx-text-fill: #4CAF50;");
        } else {
            if (studySession.getMaxUnlockedSeconds() < (int)(selectedTask.durationSeconds * 0.70)) {
                feedbackLabel.setText("❌ Incomplete watch time! You must watch at least 70% of the material.");
            } else if (correctCount < selectedTask.questions.size()) {
                feedbackLabel.setText("❌ Quiz not passed! Got " + correctCount + "/" + selectedTask.questions.size() + " correct. Please review missed material.");
            } else {
                feedbackLabel.setText("❌ Task already completed or already rewarded.");
            }
            feedbackLabel.setStyle("-fx-text-fill: #C10C4E;");
        }
    }
}
