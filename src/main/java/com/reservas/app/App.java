package com.reservas.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Logger;

public class App extends Application {

    private static final Logger logger = Logger.getLogger(App.class.getName());
    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        // Initialize the database at startup
        com.reservas.app.dao.DatabaseManager.initializeDatabase();
        
        Scene rootScene = new Scene(loadFXML("primary"), 640, 480);
        App.setAppScene(rootScene);
        
        stage.setScene(rootScene);
        stage.setTitle("Reservation System - Management");
        stage.show();
        logger.info("Application started.");
    }

    private static void setAppScene(Scene s) {
        App.scene = s;
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}
