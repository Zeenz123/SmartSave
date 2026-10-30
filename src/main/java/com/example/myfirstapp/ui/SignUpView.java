package com.example.myfirstapp.ui;

import com.example.myfirstapp.service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/** Creates a real account (own username, phone, password) in the shared database. */
public class SignUpView extends VBox {
    private final AuthService authService = AuthService.getInstance();
    private final Label statusLabel = new Label("");

    public SignUpView() {
        setSpacing(12);
        setPadding(new Insets(14, 16, 20, 16));
        setStyle("-fx-background-color: #E0F7FA;");

        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);
        Button backBtn = new Button();
        backBtn.setGraphic(IconHelper.getBackArrowIcon(Color.web("#263238"), 1.2));
        backBtn.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20px; -fx-padding: 8px; -fx-cursor: hand;");
        backBtn.setOnAction(e -> ScreenManager.getInstance().showLogin());
        Label title = new Label("Create Account");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #263238;");
        topBar.getChildren().addAll(backBtn, title);

        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #C10C4E;");

        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20px; -fx-border-radius: 20px; " +
                "-fx-border-color: #B2EBF2; -fx-padding: 18px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0.05, 0, 3);");

        TextField fullName = field("Full name");
        TextField username = field("Choose a username (e.g. riya_k)");
        TextField phone = field("Mobile number (used to find you)");
        TextField email = field("Email (optional)");
        PasswordField password = new PasswordField();
        password.setPromptText("Password (min 6 characters)");
        style(password);
        PasswordField confirm = new PasswordField();
        confirm.setPromptText("Confirm password");
        style(confirm);

        Button create = new Button("Create Account & Sign In");
        create.setPrefWidth(Double.MAX_VALUE);
        create.setStyle("-fx-background-color: #00BCD4; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; " +
                "-fx-background-radius: 16px; -fx-padding: 12px 24px; -fx-cursor: hand;");
        create.setOnAction(e -> {
            if (!password.getText().equals(confirm.getText())) {
                statusLabel.setText("Passwords do not match.");
                return;
            }
            String error = authService.validateRegistration(username.getText(), password.getText(),
                    fullName.getText(), email.getText(), phone.getText());
            if (error != null) {
                statusLabel.setText(error);
                return;
            }
            boolean ok = authService.register(username.getText(), password.getText(), fullName.getText(),
                    email.getText(), phone.getText(), "College Undergraduate", "Computer Science, Economics");
            if (ok) {
                ScreenManager.getInstance().showHome();
            } else {
                statusLabel.setText("Could not create account. Please try again.");
            }
        });

        card.getChildren().addAll(statusLabel, label("Full name"), fullName, label("Username"), username,
                label("Mobile number"), phone, label("Email"), email, label("Password"), password, confirm, create);

        ScrollPane scroll = new ScrollPane(card);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-hbar-policy: never;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(topBar, scroll);
    }

    private static Label label(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #00838F;");
        return l;
    }

    private static TextField field(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        style(f);
        return f;
    }

    private static void style(TextField f) {
        f.setStyle("-fx-font-size: 14px; -fx-text-fill: #263238; -fx-background-color: #F5F7F8; -fx-border-color: #CFD8DC; " +
                "-fx-border-radius: 14px; -fx-background-radius: 14px; -fx-padding: 10px 14px;");
    }
}
