package com.example.myfirstapp.ui;

import com.example.myfirstapp.model.User;
import com.example.myfirstapp.service.AiAssistantService;
import com.example.myfirstapp.service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Intelligent AI Assistant modal window opened via the floating button on the main dashboard.
 */
public class AiAssistantDialog {
    private final AiAssistantService aiService = new AiAssistantService();
    private final AuthService authService = AuthService.getInstance();

    public void show() {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("SmartSave AI Assistant");

        VBox layout = new VBox(14);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #E0F7FA;");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label("SmartSave AI Coach");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #00838F;");
        
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button apiKeyBtn = new Button(com.example.myfirstapp.ai.GeminiAiService.getInstance().isApiKeyConfigured() ? "✨ Gemini Active" : "🔑 Gemini Key");
        apiKeyBtn.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #00BCD4; -fx-text-fill: #00838F; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 12px; -fx-border-radius: 12px; -fx-cursor: hand;");

        header.getChildren().addAll(
                IconHelper.getAiSparkleIcon(Color.web("#00BCD4"), 1.4),
                titleLabel,
                spacer,
                apiKeyBtn
        );

        // Chat / Response Area
        TextArea chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setPrefRowCount(14);
        chatArea.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-background-radius: 12px;");

        apiKeyBtn.setOnAction(e -> {
            javafx.scene.control.TextInputDialog td = new javafx.scene.control.TextInputDialog(
                    com.example.myfirstapp.ai.GeminiAiService.getInstance().getApiKey() != null ? com.example.myfirstapp.ai.GeminiAiService.getInstance().getApiKey() : "");
            td.setTitle("Google Gemini API Setup");
            td.setHeaderText("SmartSave AI Engine: Google Gemini 2.5 Flash");
            td.setContentText("Enter Gemini API Key (or leave blank for built-in heuristic AI):");
            td.showAndWait().ifPresent(k -> {
                com.example.myfirstapp.ai.GeminiAiService.getInstance().setApiKey(k);
                boolean active = com.example.myfirstapp.ai.GeminiAiService.getInstance().isApiKeyConfigured();
                apiKeyBtn.setText(active ? "✨ Gemini Active" : "🔑 Gemini Key");
                chatArea.appendText("\n\n🤖 [System]: " + (active ? "Google Gemini API connected successfully! Using Gemini 2.5 Flash." : "Using built-in SmartSave heuristic AI models."));
            });
        });

        User user = authService.getCurrentUser();
        chatArea.setText(aiService.getPersonalizedGreeting(user));

        // Quick Topic Chips
        HBox chips = new HBox(8);
        chips.setAlignment(Pos.CENTER_LEFT);

        Button chip1 = createChip("Early Release Rules", chatArea, "How do early withdrawal approvals work?");
        Button chip2 = createChip("Study Tasks", chatArea, "What educational tasks should I complete?");
        Button chip3 = createChip("Discipline Tips", chatArea, "Give me a tip to stop impulsive spending.");

        Button chip4 = createChip("Forecast", chatArea, "When will I reach my target?");
        Button chip5 = createChip("Spending Insights", chatArea, "Show my spending analysis");

        chips.getChildren().addAll(chip1, chip2, chip3, chip4, chip5);

        // Input Row
        HBox inputRow = new HBox(8);
        TextField promptField = new TextField();
        promptField.setPromptText("Ask anything about your savings, tasks, or rules...");
        promptField.getStyleClass().add("text-input-field");
        HBox.setHgrow(promptField, Priority.ALWAYS);

        Button sendBtn = new Button("Ask AI");
        sendBtn.setStyle("-fx-background-color: #00BCD4; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10px; -fx-cursor: hand;");

        Button speakBtn = new Button("🔊 Speak");
        speakBtn.setStyle("-fx-background-color: #00838F; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10px; -fx-cursor: hand;");
        speakBtn.setOnAction(e -> {
            String text = chatArea.getText();
            int lastIdx = text.lastIndexOf("🤖 [");
            if (lastIdx == -1) lastIdx = text.lastIndexOf("✨ [");
            if (lastIdx == -1) lastIdx = text.lastIndexOf("Hello ");
            String toSpeak = (lastIdx != -1) ? text.substring(lastIdx) : text;
            speakText(toSpeak);
        });

        sendBtn.setOnAction(e -> {
            String q = promptField.getText().trim();
            if (!q.isEmpty()) {
                chatArea.appendText("\n\n🧑 You: " + q);
                String response = aiService.answerQuestion(q, authService.getCurrentUser());
                chatArea.appendText("\n\n" + response);
                promptField.clear();
                speakText(response);
            }
        });

        inputRow.getChildren().addAll(promptField, sendBtn, speakBtn);

        layout.getChildren().addAll(header, chatArea, chips, inputRow);

        Scene scene = new Scene(layout, 480, 520);
        scene.getStylesheets().add(getClass().getResource("/com/example/myfirstapp/css/smartsave.css").toExternalForm());
        dialogStage.setScene(scene);
        dialogStage.show();
    }

    private Button createChip(String text, TextArea chatArea, String query) {
        Button chip = new Button(text);
        chip.setStyle("-fx-background-color: #B2EBF2; -fx-text-fill: #006064; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 12px; -fx-cursor: hand;");
        chip.setOnAction(e -> {
            chatArea.appendText("\n\n🧑 You: " + query);
            String response = aiService.answerQuestion(query, authService.getCurrentUser());
            chatArea.appendText("\n\n" + response);
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
}
