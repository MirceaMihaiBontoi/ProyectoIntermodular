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

    @FXML private TableView<ObservableList<StringProperty>> dynamicTable;
    @FXML private GridPane dynamicForm;

    private String tableName;
    private List<String> columnNames;
    private FormManager formManager;
    private List<Object> selectedPkValues;
    private Runnable onDataChange;

    /**
     * Entry point for this controller, called manually by PrimaryController.
     */
    public void init(String tableName) {
        this.tableName = tableName;
        this.columnNames = MetadataDAO.getColumnNames(tableName);
        
        this.formManager = new FormManager(dynamicForm, columnNames, MetadataDAO.getForeignKeys(tableName), tableName);
        formManager.build();

        new TableManager(dynamicTable, columnNames).build(values -> {
            formManager.fill(values);
            List<String> pkNames = MetadataDAO.getPrimaryKeys(tableName);
            selectedPkValues = pkNames.stream()
                .map(pk -> (Object) values.get(columnNames.indexOf(pk)))
                .toList();
        });
        
        refreshData();
    }

    /**
     * Sets the callback to be executed when data changes (insert/update/delete).
     */
    public void setOnDataChange(Runnable onDataChange) {
        this.onDataChange = onDataChange;
    }

    /**
     * Reloads data from the database into the TableView.
     */
    @FXML public void refreshData() {
        dynamicTable.setItems(GenericDAO.fetchData(tableName, columnNames));
    }

    @FXML private void clearForm() {
        formManager.clear();
        selectedPkValues = null;
    }

    @FXML private void handleSave() {
        DialogHelper.doDbAction(() -> com.reservas.app.dao.DAOProvider.insert(tableName, formManager.getAllValues()),
            "Record inserted successfully!",
            this::notifyDataChange);
    }

    @FXML private void handleUpdate() {
        List<String> pks = MetadataDAO.getPrimaryKeys(tableName);
        if (pks.isEmpty()) {
            DialogHelper.showError("Error", "No Primary Key found for this table.");
            return;
        }
        if (selectedPkValues == null || selectedPkValues.isEmpty()) {
            DialogHelper.showError("Error", "Select a row before updating.");
            return;
        }

        DialogHelper.doDbAction(() -> com.reservas.app.dao.DAOProvider.update(tableName, pks, selectedPkValues, formManager.getAllValues()),
            "Record updated!",
            this::notifyDataChange);
    }

    @FXML private void handleDelete() {
        List<String> pks = MetadataDAO.getPrimaryKeys(tableName);
        if (pks.isEmpty() || selectedPkValues == null) return;

        if (DialogHelper.showConfirmation("Confirm Deletion", "Are you sure you want to delete this record?")) {
            DialogHelper.doDbAction(() -> com.reservas.app.dao.DAOProvider.delete(tableName, pks, selectedPkValues),
                null,
                this::notifyDataChange);
        }
    }

    private void notifyDataChange() {
        if (onDataChange != null) {
            onDataChange.run();
        }
        clearForm();
    }

    public void refreshCombos() {
        formManager.refreshCombos();
    }
}
