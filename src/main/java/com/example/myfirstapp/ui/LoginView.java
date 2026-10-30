package com.example.myfirstapp.ui;

import com.example.myfirstapp.service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Real sign-in screen: credentials are checked against the shared database,
 * so the same account can be used from any device connected to it.
 */
public class LoginView extends VBox {
    private final AuthService authService = AuthService.getInstance();
    private final TextField usernameField;
    private final PasswordField passwordField;
    private final Label warningLabel;

    public LoginView() {
        setAlignment(Pos.CENTER);
        setSpacing(14);
        setPadding(new Insets(30, 28, 30, 28));
        setStyle("-fx-background-color: #E0F7FA;");

        Node logo = IconHelper.createSaveMoneyLogo(1.4);
        VBox logoContainer = new VBox(logo);
        logoContainer.setAlignment(Pos.CENTER);
        logoContainer.setPrefHeight(170);

        Label title = new Label("Save for Goodness");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-font-style: italic; -fx-text-fill: #00838F;");

        Label subtitle = new Label("Sign in with your own SmartSave ID");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #546E7A;");

        warningLabel = new Label("");
        warningLabel.setWrapText(true);
        warningLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        usernameField = new TextField();
        usernameField.setPromptText("Username / Email / Phone");
        usernameField.getStyleClass().add("text-input-field");
        usernameField.setPrefWidth(330);
        usernameField.setMaxWidth(350);

        passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.getStyleClass().add("text-input-field");
        passwordField.setPrefWidth(330);
        passwordField.setMaxWidth(350);
        passwordField.setOnAction(e -> handleLogin());

        Button loginButton = new Button("Sign In");
        loginButton.getStyleClass().add("green-button");
        loginButton.setPrefWidth(220);
        loginButton.setOnAction(e -> handleLogin());

        Label newHere = new Label("New to SmartSave?");
        newHere.setStyle("-fx-font-size: 12px; -fx-text-fill: #78909C;");

        Button signUpButton = new Button("Create New Account");
        signUpButton.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #00838F; -fx-font-weight: bold; -fx-cursor: hand; " +
                "-fx-background-radius: 16px; -fx-border-color: #00BCD4; -fx-border-radius: 16px; -fx-padding: 8 22;");
        signUpButton.setOnAction(e -> ScreenManager.getInstance().showSignUp());

        getChildren().addAll(logoContainer, title, subtitle, warningLabel, usernameField, passwordField,
                loginButton, newHere, signUpButton);
    }

    private void handleLogin() {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            showWarning("Enter your username and password.", false);
            return;
        }
        if (authService.login(user, pass)) {
            ScreenManager.getInstance().showHome();
        } else {
            showWarning("Sign in failed! Invalid username or password.", false);
        }
    }

    private void showWarning(String message, boolean ok) {
        warningLabel.setText(message);
        warningLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + (ok ? "#4CAF50" : "#C10C4E") + ";");
    }
}
