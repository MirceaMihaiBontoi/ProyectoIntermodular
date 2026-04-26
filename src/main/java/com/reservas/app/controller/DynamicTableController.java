package com.reservas.app.controller;

import com.reservas.app.dao.GenericDAO;
import com.reservas.app.dao.MetadataDAO;
import com.reservas.app.dao.UsuarioDAO;
import com.reservas.app.util.DialogHelper;
import com.reservas.app.util.FormManager;
import com.reservas.app.util.TableManager;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;

import java.util.List;

/**
 * Controller for the dynamic table view. 
 * It manages the lifecycle of a single table tab.
 */
public class DynamicTableController {

    // FXML annotations link these variables to the UI elements in dynamic_table.fxml
    @FXML private TableView<ObservableList<StringProperty>> dynamicTable;
    @FXML private GridPane dynamicForm;

    private String tableName;
    private List<String> columnNames;
    private FormManager formManager;
    private Object selectedPkValue;
    private PrimaryController primaryController;

    /**
     * Entry point for this controller, called manually by PrimaryController.
     */
    public void init(String tableName) {
        this.tableName = tableName;
        
        // 1. Get metadata from the DB
        this.columnNames = MetadataDAO.getColumnNames(tableName);
        
        // 2. Setup the Form Manager (controls the input fields)
        this.formManager = new FormManager(dynamicForm, columnNames, MetadataDAO.getForeignKeys(tableName), tableName);
        formManager.build();

        // 3. Setup the Table Manager (controls columns and selection)
        // Capture the original PK so updates use it even if the user edits the PK field
        new TableManager(dynamicTable, columnNames).build(values -> {
            formManager.fill(values);
            String pk = MetadataDAO.getPrimaryKey(tableName);
            selectedPkValue = (pk != null) ? values.get(columnNames.indexOf(pk)) : null;
        });
        
        // 4. Initial data load
        refreshData();
    }

    /**
     * Sets the reference to the primary controller for global refresh operations.
     */
    public void setPrimaryController(PrimaryController primaryController) {
        this.primaryController = primaryController;
    }

    /**
     * Reloads data from the database into the TableView.
     */
    @FXML public void refreshData() {
        dynamicTable.setItems(GenericDAO.fetchData(tableName, columnNames));
    }

    @FXML private void clearForm() {
        formManager.clear();
        selectedPkValue = null;
    }

    /**
     * Patterns: Action -> Feedback -> Cleanup
     */
    @FXML private void handleSave() {
        // Special handling for usuario table with cascade logic
        if ("usuario".equals(tableName)) {
            DialogHelper.doDbAction(() -> UsuarioDAO.insertWithCascade(formManager.getAllValues()),
                "Record inserted successfully!",
                () -> { refreshAllData(); refreshAllCombos(); clearForm(); });
        } else {
            // Standard insert for other tables
            DialogHelper.doDbAction(() -> GenericDAO.insert(tableName, formManager.getAllValues()),
                "Record inserted successfully!",
                () -> { refreshAllData(); refreshAllCombos(); clearForm(); });
        }
    }

    @FXML private void handleUpdate() {
        String pk = MetadataDAO.getPrimaryKey(tableName);
        if (pk == null) {
            DialogHelper.showError("Error", "No Primary Key found for this table.");
            return;
        }
        if (selectedPkValue == null) {
            DialogHelper.showError("Error", "Select a row before updating.");
            return;
        }

        // Special handling for usuario table with cascade logic
        if ("usuario".equals(tableName)) {
            DialogHelper.doDbAction(() -> UsuarioDAO.updateWithCascade(pk, selectedPkValue, formManager.getAllValues()),
                "Record updated!",
                () -> { refreshAllData(); refreshAllCombos(); });
        } else {
            // Standard update for other tables
            DialogHelper.doDbAction(() -> GenericDAO.update(tableName, pk, selectedPkValue, formManager.getAllValues()),
                "Record updated!",
                () -> { refreshAllData(); refreshAllCombos(); });
        }
    }

    @FXML private void handleDelete() {
        String pk = MetadataDAO.getPrimaryKey(tableName);
        if (pk == null || selectedPkValue == null) return;

        if (DialogHelper.showConfirmation("Confirm Deletion", "Are you sure you want to delete this record?")) {
            // Special handling for usuario table with cascade logic
            if ("usuario".equals(tableName)) {
                DialogHelper.doDbAction(() -> UsuarioDAO.deleteWithCascade(pk, selectedPkValue),
                    null,
                    () -> { refreshAllData(); refreshAllCombos(); clearForm(); });
            } else {
                // Standard delete for other tables
                DialogHelper.doDbAction(() -> GenericDAO.delete(tableName, pk, selectedPkValue),
                    null,
                    () -> { refreshAllData(); refreshAllCombos(); clearForm(); });
            }
        }
    }

    /**
     * Refreshes all ComboBox dropdowns with fresh data from the database.
     * This ensures that when new records are added (e.g., new users), they appear in dropdowns.
     */
    public void refreshCombos() {
        formManager.refreshCombos();
    }

    /**
     * Refreshes all ComboBox dropdowns across all tabs globally.
     * This ensures that when new records are added in one tab, they appear in dropdowns of other tabs.
     */
    private void refreshAllCombos() {
        if (primaryController != null) {
            primaryController.refreshAllCombos();
        }
    }

    /**
     * Refreshes all table data across all tabs globally.
     * This ensures that when CASCADE deletes occur, all affected tabs show updated data.
     */
    private void refreshAllData() {
        if (primaryController != null) {
            primaryController.refreshAllData();
        }
    }
}
