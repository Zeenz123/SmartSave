package com.example.myfirstapp;

import com.example.myfirstapp.dao.DatabaseManager;
import com.example.myfirstapp.ui.ScreenManager;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main entry point for the SmartSave JavaFX Application.
 */
public class SmartSaveApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Initialize persistent database
        DatabaseManager.getInstance();

        // Initialize UI screen manager and primary stage
        ScreenManager.getInstance().init(primaryStage);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
