package com.reservas.app.controller;

import com.reservas.app.dao.GenericDAO;
import com.reservas.app.dao.MetadataDAO;
import com.reservas.app.util.DialogHelper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PrimaryController {

    private static final Logger logger = Logger.getLogger(PrimaryController.class.getName());

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
        reloadTabs();
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
                
                // Fun logic: if a table has 2+ Foreign Keys, it's likely a relationship table (junction)
                boolean isJunction = MetadataDAO.getForeignKeys(tableName).size() >= 2;
                String title = (isJunction ? "🔗 " : "") + tableName.toUpperCase();
                
                Tab tab = new Tab(title, root);
                mainTabPane.getTabs().add(tab);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error loading tab: " + tableName, e);
            }
        }
    }

    /**
     * Executing SQL from the text area in the app.
     * This allows you to run commands like 'DROP TABLE' or 'UPDATE' manually.
     */
    @FXML
    private void executeSql() {
        String sql = sqlConsole.getText().trim();
        if (sql.isEmpty()) return;

        try {
            GenericDAO.executeRawSql(sql);
            DialogHelper.showInfo("SQL Executed", "Statement executed successfully.");
            sqlConsole.clear();
            // Only reload tabs if the SQL changed the schema (DDL)
            if (isSchemaChange(sql)) {
                reloadTabs();
            }
        } catch (SQLException e) {
            DialogHelper.showError("SQL Error", e.getMessage());
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
        Platform.exit();
    }
}
