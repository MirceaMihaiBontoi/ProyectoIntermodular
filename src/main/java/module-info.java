module com.reservas.app {
    // DEPENDENCIES: We tell Java which libraries our app needs to run.
    requires transitive javafx.controls; // Basic UI components like buttons, tables.
    requires javafx.fxml;                // Needed to load layouts from .fxml files.
    requires transitive java.sql;        // Needed to connect to the SQLite database.

    /**
     * REFLECTION: JavaFX needs to "look inside" our controller package to link 
     * the buttons in the FXML to the methods in our code.
     */
    opens com.reservas.app to javafx.fxml, javafx.controls;
    opens com.reservas.app.dao to javafx.fxml;
    opens com.reservas.app.controller to javafx.fxml;
    opens com.reservas.app.util to javafx.fxml;
    opens com.reservas.app.web to ALL-UNNAMED;

    // EXPORTS: We make our packages visible to the JavaFX system and other modules.
    exports com.reservas.app;            // Contains the main App.java entry point.
    exports com.reservas.app.controller; // Contains JavaFX controllers (Logic).
    exports com.reservas.app.dao;        // Contains Database access classes (Data).
    exports com.reservas.app.model;       // Contains Data classes (Entities).
}
