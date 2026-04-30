package com.reservas.app;

import com.reservas.app.controller.PrimaryController;
import com.reservas.app.dao.DatabaseManager;
import com.reservas.app.web.WebServer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main entry point for the Reservation System JavaFX application.
 * Manages the application lifecycle, database initialization, and web server integration.
 */
public class App extends Application {

    private static final Logger logger = Logger.getLogger(App.class.getName());
    
    // Configuration Constants
    private static final int DEFAULT_PORT = 3000;
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;
    private static final String APP_TITLE = "Reservation System";
    private static final String PRIMARY_FXML = "primary";

    private PrimaryController primaryController;

    /**
     * Initializes the application and UI.
     * @param stage The primary stage for this application.
     */
    @Override
    public void start(Stage stage) {
        try {
            // 1. Core Services Initialization
            DatabaseManager.initializeDatabase();
            
            // 2. UI Setup (must be done before registering callbacks that depend on it)
            setupUI(stage);
            
            // 3. Start Web Server and register UI refresh callback
            startWebServer();
            WebServer.setOnDataChange(() -> {
                if (primaryController != null) {
                    Platform.runLater(() -> {
                        primaryController.refreshAllData();
                        primaryController.refreshAllCombos();
                    });
                }
            });
            
            logger.info("Application started successfully.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to start application", e);
            Platform.exit();
        }
    }

    /**
     * Starts the Javalin web server in a separate daemon thread.
     */
    private void startWebServer() {
        Thread webThread = new Thread(() -> WebServer.start(DEFAULT_PORT));
        webThread.setDaemon(true); // Ensures the thread dies when the main app exits
        webThread.setName("WebServer-Thread");
        webThread.start();
        logger.info("Web server initiated on port " + DEFAULT_PORT);
    }

    /**
     * Configures the primary stage and loads the initial scene.
     */
    private void setupUI(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/reservas/app/" + PRIMARY_FXML + ".fxml"));
        Parent root = loader.load();
        this.primaryController = loader.getController();

        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        stage.setScene(scene);
        stage.setTitle(APP_TITLE);
        
        // Ensure clean exit when window is closed
        stage.setOnCloseRequest(event -> {
            logger.info("Close request received, exiting platform...");
            Platform.exit();
        });
        
        stage.show();
    }

    /**
     * Cleanly stops all background services when the application exits.
     * This is automatically called by the JavaFX platform.
     */
    @Override
    public void stop() {
        logger.info("Stopping application services...");
        WebServer.stop();
    }


    public static void main(String[] args) {
        launch(args);
    }
}

