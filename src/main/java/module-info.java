module com.reservas.app {
    // DEPENDENCIES: We tell Java which libraries our app needs to run.
    requires transitive javafx.controls; // Basic UI components like buttons, tables.
    requires javafx.fxml;                // Needed to load layouts from .fxml files.
    requires transitive java.sql;        // Needed to connect to the SQLite database.
    requires java.logging;               // Needed for logging events.
    requires java.desktop;               // Needed for java.awt.Desktop (browser opening).
    
    // Web, JSON and Security dependencies
    requires io.javalin;
    requires com.google.gson;
    requires bcrypt;

    /**
     * REFLECTION: JavaFX needs to "look inside" our controller package to link 
     * the buttons in the FXML to the methods in our code.
     */
    opens com.reservas.app to javafx.fxml, javafx.controls;
    opens com.reservas.app.dao to javafx.fxml;
    opens com.reservas.app.controller to javafx.fxml;
    opens com.reservas.app.util to javafx.fxml;
    
    // Allow Javalin and Gson to access our web and model packages via reflection
    opens com.reservas.app.web to io.javalin, com.google.gson;
    opens com.reservas.app.model to com.google.gson;

    // EXPORTS: We make our packages visible to the JavaFX system and other modules.
    exports com.reservas.app;            // Contains the main App.java entry point.
    exports com.reservas.app.controller; // Contains JavaFX controllers (Logic).
    exports com.reservas.app.dao;        // Contains Database access classes (Data).
    exports com.reservas.app.model;       // Contains Data classes (Entities).
}
