package com.reservas.app.util;

import com.reservas.app.dao.MetadataDAO;
import com.reservas.app.model.ForeignKey;
import com.reservas.app.util.fields.ComboFormField;
import com.reservas.app.util.fields.DateFormField;
import com.reservas.app.util.fields.IFormField;
import com.reservas.app.util.fields.TextFormField;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrator that manages the dynamic form UI.
 * It builds the layout and handles data synchronization between the UI and the DAO.
 */
public class FormManager {

    private final GridPane form;
    private final List<String> columnNames;
    private final Map<String, ForeignKey> foreignKeys;
    private final String tableName;
    // Polymorphic Map: Stores IFormField instead of specific controls
    private final Map<String, IFormField> fieldsMap = new HashMap<>();

    public FormManager(GridPane form, List<String> columnNames, Map<String, ForeignKey> foreignKeys, String tableName) {
        this.form = form;
        this.columnNames = columnNames;
        this.foreignKeys = foreignKeys;
        this.tableName = tableName;
    }

    /**
     * Builds the form layout dynamically based on column names and foreign keys.
     * 
     * HOW IT WORKS:
     * 1. Clear the old UI.
     * 2. Setup a Grid (rows and columns).
     * 3. We define "ColumnConstraints" to control alignment and sizing.
     * 4. For each database column, we create a Label and an Input Field.
     */
    public void build() {
        form.getChildren().clear();
        form.getColumnConstraints().clear();
        fieldsMap.clear();

        // Setup 4 columns: 
        // [Label 1] [Field 1] | [Label 2] [Field 2]
        for (int i = 0; i < 2; i++) {
            ColumnConstraints labelCol = new ColumnConstraints();
            labelCol.setMinWidth(110);
            labelCol.setHalignment(javafx.geometry.HPos.RIGHT); // Labels align to the right
            
            ColumnConstraints fieldCol = new ColumnConstraints();
            fieldCol.setHgrow(Priority.ALWAYS); // Fields grow to take available space
            fieldCol.setMinWidth(150);
            
            form.getColumnConstraints().addAll(labelCol, fieldCol);
        }
        
        form.setHgap(15); // Horizontal space between cells
        form.setVgap(12); // Vertical space between cells

        // Add fields to the grid
        for (int i = 0; i < columnNames.size(); i++) {
            String name = columnNames.get(i);
            Label label = new Label(name.toUpperCase() + ":");
            label.setStyle("-fx-font-weight: bold;");
            
            // POLYMORPHISM IN ACTION: 
            // We call createField() which returns an 'IFormField' interface.
            // We don't care if it's a TextField or a ComboBox!
            IFormField field = createField(name);
            fieldsMap.put(name, field);
            
            // Grid indexing:
            // Column = (i % 2) * 2 -> alternates between 0 and 2 for labels
            // Row = i / 2 -> increases every 2 items
            form.add(label, (i % 2) * 2, i / 2);
            form.add(field.getControl(), (i % 2) * 2 + 1, i / 2);
        }
    }

    /**
     * Internal Factory Method:
     * A "Factory" is a design pattern used to create objects.
     * Priority order:
     * 1. If the column is a Foreign Key (FK), we need a ComboBox (Dropdown)
     * 2. If the column has allowed values (CHECK constraint), use ComboBox
     * 3. If the column is a DATE type, we use a DatePicker
     * 4. Otherwise, a normal TextFormField is enough.
     */
    private IFormField createField(String name) {
        // Priority 1: Foreign Key - use ComboBox
        if (foreignKeys.containsKey(name)) {
            ForeignKey fk = foreignKeys.get(name);
            // We fetch the valid values from the parent table to populate the dropdown
            List<String> items = MetadataDAO.getAllRowsFromTable(fk.refTable(), fk.refColumn());
            return new ComboFormField(items);
        }
        
        // Priority 2: CHECK constraint with allowed values - use ComboBox
        List<String> allowedValues = MetadataDAO.getAllowedValues(tableName, name);
        if (allowedValues != null && !allowedValues.isEmpty()) {
            return new ComboFormField(allowedValues);
        }
        
        // Priority 3: DATE type - use DatePicker
        String columnType = MetadataDAO.getColumnType(tableName, name);
        if (columnType != null && columnType.equalsIgnoreCase("DATE")) {
            return new DateFormField();
        }
        
        // Priority 4: Default - use TextField
        return new TextFormField();
    }

    /**
     * Fills the entire form with values from a selected record.
     * Thanks to Polymorphism, we just call .setValue() on each field.
     */
    public void fill(List<String> values) {
        for (int i = 0; i < columnNames.size(); i++) {
            fieldsMap.get(columnNames.get(i)).setValue(values.get(i));
        }
    }

    /**
     * Resets all fields in the form.
     */
    public void clear() {
        // High-level abstraction: we tell all fields to clear themselves
        fieldsMap.values().forEach(IFormField::clear);
    }

    /**
     * Retrieves the data from a specific field.
     */
    public Object getValue(String name) {
        return fieldsMap.get(name).getValue();
    }

    /**
     * Gathers all form data into a Map (ColumnName -> Data).
     * This Map is what GenericDAO uses to build the SQL query.
     */
    public Map<String, Object> getAllValues() {
        Map<String, Object> values = new HashMap<>();
        for (String name : columnNames) {
            values.put(name, getValue(name));
        }
        return values;
    }

    /**
     * Refreshes all ComboBox fields with fresh data from the database.
     * This is called after inserting/updating records to ensure dropdowns show the latest data.
     * Refreshes both foreign key dropdowns and CHECK constraint dropdowns.
     */
    public void refreshCombos() {
        for (String columnName : columnNames) {
            IFormField field = fieldsMap.get(columnName);
            if (field instanceof ComboFormField) {
                // Refresh foreign key dropdowns
                if (foreignKeys.containsKey(columnName)) {
                    ForeignKey fk = foreignKeys.get(columnName);
                    List<String> freshItems = MetadataDAO.getAllRowsFromTable(fk.refTable(), fk.refColumn());
                    ((ComboFormField) field).refreshItems(freshItems);
                }
                // Refresh CHECK constraint dropdowns (allowed values don't change, but we refresh anyway for consistency)
                else {
                    List<String> allowedValues = MetadataDAO.getAllowedValues(tableName, columnName);
                    if (allowedValues != null) {
                        ((ComboFormField) field).refreshItems(allowedValues);
                    }
                }
            }
        }
    }
}
