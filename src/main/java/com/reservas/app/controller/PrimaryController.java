package com.reservas.app.controller;

import com.reservas.app.dao.GenericDAO;
import com.reservas.app.dao.MetadataDAO;
import com.reservas.app.dao.SqlConsoleSupport;
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

    @FXML
    private TabPane mainTabPane;
    @FXML
    private TextArea sqlConsole;

    /**
     * The initialize method is automatically called by JavaFX after the FXML is loaded.
     */
    @FXML
    public void initialize() {
        reloadTabs();
    }

    /**
     * Refreshes all ComboBox dropdowns across all tabs.
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
     * Refreshes all table data across all tabs (JavaFX only).
     */
    public void refreshAllData() {
        for (Tab tab : mainTabPane.getTabs()) {
            Object controller = tab.getUserData();
            if (controller instanceof DynamicTableController) {
                ((DynamicTableController) controller).refreshData();
            }
        }
    }

    /**
     * This is the CORE of the dynamic UI:
     * 1. Ask the database for all table names.
     * 2. For each table, create a new Tab.
     */
    @FXML
    private void reloadTabs() {
        mainTabPane.getTabs().clear();
        List<String> tables = MetadataDAO.getTableNames();

        for (String tableName : tables) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/reservas/app/dynamic_table.fxml"));
                Parent root = loader.load();
                
                DynamicTableController controller = loader.getController();
                controller.init(tableName);
                // Instead of passing 'this', we pass the specific refresh methods to avoid tight coupling
                controller.setOnDataChange(() -> {
                    refreshAllData();
                    refreshAllCombos();
                    WebServer.notifyWeb();
                });
                
                boolean isJunction = MetadataDAO.getForeignKeys(tableName).size() >= 2;
                String title = (isJunction ? "🔗 " : "") + tableName.toUpperCase();
                
                Tab tab = new Tab(title, root);
                tab.setUserData(controller);
                mainTabPane.getTabs().add(tab);
            } catch (IOException e) {
                logger.log(Level.SEVERE, e, () -> "Error loading tab: " + tableName);
            }
        }
    }

    /**
     * Executing SQL from the text area in the app.
     */
    @FXML
    private void executeSql() {
        String sql = sqlConsole.getText().trim();
        if (sql.isEmpty()) return;

        try {
            VBox resultsContainer = new VBox(10);
            boolean hasSchemaChange = false;
            boolean hasResults = false;

            for (String statement : SqlConsoleSupport.statements(sql)) {
                if (SqlConsoleSupport.isSelect(statement)) {
                    resultsContainer.getChildren().add(buildSqlResultTable(statement));
                    hasResults = true;
                } else {
                    int affected = GenericDAO.executeRawUpdate(statement);
                    Label label = new Label(statement.substring(0, Math.min(40, statement.length())) + "... → " + affected + " row(s) affected");
                    label.setStyle("-fx-font-weight: bold; -fx-text-fill: #2e7d32;");
                    resultsContainer.getChildren().add(label);
                    if (SqlConsoleSupport.isSchemaChange(statement)) hasSchemaChange = true;
                    hasResults = true;
                }
            }
            
            if (hasResults) showResultsTab(resultsContainer);
            // If tables were created/dropped, rebuild tabs first so refreshAllData does not hit stale tab names.
            if (hasSchemaChange) reloadTabs();
            refreshAllData();
            refreshAllCombos();
            WebServer.notifyWeb();
            
            sqlConsole.clear();
        } catch (SQLException e) {
            DialogHelper.showError("SQL Error", e.getMessage());
        }
    }

    private Node buildSqlResultTable(String sql) throws SQLException {
        TableView<ObservableList<String>> tableView = new TableView<>();
        tableView.setPrefHeight(200);

        try (Connection conn = com.reservas.app.dao.DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                final int colIndex = i;
                TableColumn<ObservableList<String>, String> column = new TableColumn<>(metaData.getColumnName(i));
                column.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().get(colIndex - 1)));
                tableView.getColumns().add(column);
            }

            ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
            while (rs.next()) {
                ObservableList<String> row = FXCollections.observableArrayList();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getString(i));
                }
                data.add(row);
            }
            tableView.setItems(data);
        }

        VBox container = new VBox(5);
        Label sqlLabel = new Label(sql);
        sqlLabel.setStyle("-fx-font-family: monospace; -fx-font-size: 11px; -fx-text-fill: #666;");
        container.getChildren().addAll(sqlLabel, tableView);
        return container;
    }

    private void showResultsTab(Node content) {
        Tab resultTab = mainTabPane.getTabs().stream()
                .filter(t -> "📊 SQL Results".equals(t.getText()))
                .findFirst()
                .orElseGet(() -> {
                    Tab t = new Tab("📊 SQL Results");
                    t.setClosable(true);
                    mainTabPane.getTabs().add(t);
                    return t;
                });
        resultTab.setContent(content);
        mainTabPane.getSelectionModel().select(resultTab);
    }

    @FXML
    private void exitApp() {
        logger.info("Application closing...");
        WebServer.stop();
        Platform.exit();
    }

    @FXML
    private void openWebInterface() {
        String url = "http://localhost:3000";
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            } else {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) new ProcessBuilder("cmd", "/c", "start", url).start();
                else if (os.contains("mac")) new ProcessBuilder("open", url).start();
                else new ProcessBuilder("xdg-open", url).start();
            }
        } catch (Exception e) {
            DialogHelper.showError("Error", "Could not open browser: " + e.getMessage());
        }
    }
}
