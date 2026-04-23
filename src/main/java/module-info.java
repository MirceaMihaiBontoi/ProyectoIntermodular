module com.reservas.app {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires javafx.base; // Required for TableView property access

    opens com.reservas.app.controller to javafx.fxml;
    opens com.reservas.app.model to javafx.base; // Opens model to reflection for TableView
    exports com.reservas.app;
}
