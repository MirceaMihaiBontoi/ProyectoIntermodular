package com.reservas.app.controller;

import com.reservas.app.dao.GenericDAO;
import com.reservas.app.dao.MetadataDAO;
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

    /**
     * Entry point for this controller, called manually by PrimaryController.
     */
    public void init(String tableName) {
        this.tableName = tableName;
        
        // 1. Get metadata from the DB
        this.columnNames = MetadataDAO.getColumnNames(tableName);
        
        // 2. Setup the Form Manager (controls the input fields)
        this.formManager = new FormManager(dynamicForm, columnNames, MetadataDAO.getForeignKeys(tableName));
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
        // doDbAction handles the try-catch and alerts for us
        DialogHelper.doDbAction(() -> GenericDAO.insert(tableName, formManager.getAllValues()), 
            "Record inserted successfully!", 
            () -> { refreshData(); clearForm(); });
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

        DialogHelper.doDbAction(() -> GenericDAO.update(tableName, pk, selectedPkValue, formManager.getAllValues()),
            "Record updated!",
            this::refreshData);
    }

    @FXML private void handleDelete() {
        String pk = MetadataDAO.getPrimaryKey(tableName);
        if (pk == null || selectedPkValue == null) return;

        if (DialogHelper.showConfirmation("Confirm Deletion", "Are you sure you want to delete this record?")) {
            DialogHelper.doDbAction(() -> GenericDAO.delete(tableName, pk, selectedPkValue),
                null,
                () -> { refreshData(); clearForm(); });
        }
    }
}
