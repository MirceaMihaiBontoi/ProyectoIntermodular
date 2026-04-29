package com.reservas.app.controller;

import com.reservas.app.dao.MetadataDAO;
import com.reservas.app.util.DialogHelper;
import com.reservas.app.web.WebServer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PrimaryController {

    private static final Logger logger = Logger.getLogger(PrimaryController.class.getName());
    private static PrimaryController instance;

    @FXML
    private TabPane mainTabPane;
    @FXML
    private TextArea sqlConsole;
    @FXML
    private VBox sidebar;

    /**
     * The initialize method is automatically called by JavaFX after the FXML is loaded.
     * It's like a constructor for the UI.
     */
    @FXML
    public void initialize() {
        setInstance(this);
        reloadTabs();
    }

    private static void setInstance(PrimaryController controller) {
        instance = controller;
    }

    /**
     * Returns the singleton instance of PrimaryController for external access.
     */
    public static PrimaryController getInstance() {
        return instance;
    }

    /**
     * Refreshes all ComboBox dropdowns across all tabs.
     * This ensures that when new records are added in one tab, they appear in dropdowns of other tabs.
     */
    public void refreshAllCombos() {
        for (Tab tab : mainTabPane.getTabs()) {
            Object controller = tab.getUserData();
            if (controller instanceof DynamicTableController) {
                ((DynamicTableController) controller).refreshCombos();
            }
        }
    }

    /**
     * Refreshes all table data across all tabs.
     * This ensures that when CASCADE deletes occur, all affected tabs show updated data.
     */
    public void refreshAllData() {
        for (Tab tab : mainTabPane.getTabs()) {
            Object controller = tab.getUserData();
            if (controller instanceof DynamicTableController) {
                ((DynamicTableController) controller).refreshData();
            }
        }
        // Notify web clients that data has changed in the UI
        WebServer.notifyWeb();
    }

    /**
     * This is the CORE of the dynamic UI:
     * 1. Ask the database for all table names.
     * 2. For each table, create a new Tab.
     * 3. Load the 'dynamic_table.fxml' into that tab.
     * 4. Assign a controller to manage the data of that specific table.
     */
    @FXML
    private void reloadTabs() {
        // Clear existing tabs to avoid duplicates if we reload
        mainTabPane.getTabs().clear();
        List<String> tables = MetadataDAO.getTableNames();

        for (String tableName : tables) {
            try {
                // FXMLLoader is used to load UI layouts from .fxml files
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/reservas/app/dynamic_table.fxml"));
                Parent root = loader.load();
                
                // We get the controller of the loaded FXML to initialize it with the table name
                DynamicTableController controller = loader.getController();
                controller.init(tableName);
                controller.setPrimaryController(this); // Set reference for global refresh
                
                // Fun logic: if a table has 2+ Foreign Keys, it's likely a relationship table (junction)
                boolean isJunction = MetadataDAO.getForeignKeys(tableName).size() >= 2;
                String title = (isJunction ? "🔗 " : "") + tableName.toUpperCase();
                
                Tab tab = new Tab(title, root);
                tab.setUserData(controller); // Store controller reference for later access
                mainTabPane.getTabs().add(tab);
            } catch (IOException e) {
                logger.log(Level.SEVERE, e, () -> "Error loading tab: " + tableName);
            }
        }
    }

    /**
     * Executing SQL from the text area in the app.
     * This allows you to run commands like 'DROP TABLE' or 'UPDATE' manually.
     * Supports multiple statements separated by semicolons.
     */
    @FXML
    private void executeSql() {
        String sql = sqlConsole.getText().trim();
        if (sql.isEmpty()) return;

        try {
            // Split SQL by semicolon and execute each statement
            SqlExecutionResult result = executeSqlStatements(sql);
            
            if (result.hasResults()) {
                showResultsTab(result.getResultsContainer());
            } else {
                DialogHelper.showInfo("SQL Executed", "Statement executed successfully.");
            }

            refreshAllData();
            refreshAllCombos();
            
            if (result.hasSchemaChange()) {
                reloadTabs();
            }

            sqlConsole.clear();
        } catch (SQLException e) {
            DialogHelper.showError("SQL Error", e.getMessage());
        }
    }

    private SqlExecutionResult executeSqlStatements(String sql) throws SQLException {
        String[] statements = sql.split(";");
        VBox resultsContainer = new VBox(10);
        boolean hasSelect = false;
        boolean hasSchemaChange = false;

        for (String statement : statements) {
            statement = statement.trim();
            if (statement.isEmpty()) continue;

            String upper = statement.toUpperCase();
            if (upper.startsWith("SELECT")) {
                hasSelect = true;
                resultsContainer.getChildren().add(executeSelectQuery(statement));
            } else {
                int affectedRows = executeNonQuery(statement);
                Label resultLabel = new Label(statement.substring(0, Math.min(50, statement.length())) + "... → " + affectedRows + " fila(s) afectada(s)");
                resultLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2e7d32;");
                resultsContainer.getChildren().add(resultLabel);
                if (isSchemaChange(statement)) {
                    hasSchemaChange = true;
                }
            }
        }

        return new SqlExecutionResult(resultsContainer, hasSelect, hasSchemaChange);
    }

    private static class SqlExecutionResult {
        private final VBox resultsContainer;
        private final boolean hasSelect;
        private final boolean hasSchemaChange;

        SqlExecutionResult(VBox resultsContainer, boolean hasSelect, boolean hasSchemaChange) {
            this.resultsContainer = resultsContainer;
            this.hasSelect = hasSelect;
            this.hasSchemaChange = hasSchemaChange;
        }

        boolean hasResults() {
            return hasSelect || !resultsContainer.getChildren().isEmpty();
        }

        VBox getResultsContainer() {
            return resultsContainer;
        }

        boolean hasSchemaChange() {
            return hasSchemaChange;
        }
    }

    /**
     * Executes a SELECT query and returns a TableView with results.
     */
    private Node executeSelectQuery(String sql) throws SQLException {
        try (Connection conn = com.reservas.app.dao.DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // Create TableView
            TableView<ObservableList<String>> tableView = new TableView<>();
            tableView.setPrefHeight(200);

            // Create columns
            for (int i = 1; i <= columnCount; i++) {
                final int colIndex = i;
                TableColumn<ObservableList<String>, String> column = new TableColumn<>(metaData.getColumnName(i));
                column.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().get(colIndex - 1)));
                tableView.getColumns().add(column);
            }

            // Add data
            ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
            while (rs.next()) {
                ObservableList<String> row = FXCollections.observableArrayList();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getString(i));
                }
                data.add(row);
            }
            tableView.setItems(data);

            // Wrap in VBox with SQL statement label
            VBox container = new VBox(5);
            Label sqlLabel = new Label(sql);
            sqlLabel.setStyle("-fx-font-family: monospace; -fx-font-size: 11px; -fx-text-fill: #666;");
            container.getChildren().addAll(sqlLabel, tableView);
            return container;
        }
    }

    /**
     * Executes a non-SELECT query (INSERT, UPDATE, DELETE, etc.) and returns affected rows.
     */
    private int executeNonQuery(String sql) throws SQLException {
        try (Connection conn = com.reservas.app.dao.DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            return stmt.executeUpdate(sql);
        }
    }

    /**
     * Shows results in a temporary tab.
     */
    private void showResultsTab(Node content) {
        // Check if SQL Results tab already exists
        Tab existingTab = null;
        for (Tab tab : mainTabPane.getTabs()) {
            if (tab.getText().equals("📊 SQL Results")) {
                existingTab = tab;
                break;
            }
        }

        if (existingTab != null) {
            existingTab.setContent(content);
            mainTabPane.getSelectionModel().select(existingTab);
        } else {
            Tab resultTab = new Tab("📊 SQL Results", content);
            resultTab.setClosable(true);
            mainTabPane.getTabs().add(resultTab);
            mainTabPane.getSelectionModel().select(resultTab);
        }
    }

    private boolean isSchemaChange(String sql) {
        String upper = sql.trim().toUpperCase();
        return upper.startsWith("CREATE") || upper.startsWith("DROP") || upper.startsWith("ALTER");
    }

    /**
     * Clean way to close a JavaFX application.
     */
    @FXML
    private void exitApp() {
        logger.info("Application closing via exit button, stopping web server...");
        WebServer.stop();
        Platform.exit();
        System.exit(0); // Force exit
    }

    /**
     * Opens the web interface in the default browser.
     */
    @FXML
    private void openWebInterface() {
        try {
            // Use ProcessBuilder to open browser without requiring java.desktop module
            String os = System.getProperty("os.name").toLowerCase();
            String url = "http://localhost:3000";
            
            if (os.contains("win")) {
                new ProcessBuilder("cmd", "/c", "start", url).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
            } else {
                new ProcessBuilder("xdg-open", url).start();
            }
        } catch (Exception e) {
            DialogHelper.showError("Error", "Could not open web interface: " + e.getMessage());
        }
    }
}
