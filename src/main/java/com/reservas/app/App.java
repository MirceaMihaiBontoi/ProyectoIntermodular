package com.reservas.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.reservas.app.dao.DatabaseManager;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Main class of the JavaFX application.
 * It extends 'Application', which is the base class for all JavaFX apps.
 */
public class App extends Application {

    private static final Logger logger = Logger.getLogger(App.class.getName());

    /**
     * The 'start' method is the main entry point for all JavaFX applications.
     * It is called after the system is ready for the application to begin running.
     */
    @Override
    public void start(Stage stage) throws IOException {
        // Initialize the database connection and schema before the UI starts
        DatabaseManager.initializeDatabase();
        
        logger.info("Application started.");
        
        // Load the main FXML layout and display it in the primary stage (window)
        Scene scene = new Scene(loadFXML("primary"), 1200, 800);
        stage.setScene(scene);
        stage.setTitle("Sistemas Reservas");
        stage.show();
    }

    /**
     * Utility method to load FXML files from the resources folder.
     */
    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        // launch() starts the JavaFX lifecycle
        launch();
    }
}
