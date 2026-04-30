package com.reservas.app.util;

import com.reservas.app.dao.MetadataDAO;
import com.reservas.app.model.ForeignKey;
import com.reservas.app.util.fields.ComboFormField;
import com.reservas.app.util.fields.DateFormField;
import com.reservas.app.util.fields.DisplayComboFormField;
import com.reservas.app.util.fields.IFormField;
import com.reservas.app.util.fields.TextFormField;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

/**
 * Orchestrator that manages the dynamic form UI.
 * It builds the layout and handles data synchronization between the UI and the DAO.
 * Now supports dynamic visibility for subtype fields (e.g., Administrador/Normal user).
 */
public class FormManager {

    private final GridPane form;
    private final List<String> columnNames;
    private final Map<String, ForeignKey> foreignKeys;
    private final String tableName;

    private final Map<String, IFormField> fieldsMap = new HashMap<>();
    private final Map<String, Label> labelsMap = new HashMap<>();
    private boolean updateMode;

    public FormManager(
        GridPane form,
        List<String> columnNames,
        Map<String, ForeignKey> foreignKeys,
        String tableName
    ) {
        this.form = form;
        this.columnNames = columnNames;
        this.foreignKeys = foreignKeys;
        this.tableName = tableName;
    }

    public void build() {
        form.getChildren().clear();
        form.getColumnConstraints().clear();
        fieldsMap.clear();
        labelsMap.clear();

        setupColumns();

        form.setHgap(15);
        form.setVgap(12);

        int row = 0;
        for (int i = 0; i < columnNames.size(); i++) {
            String name = columnNames.get(i);
            Label label = createLabel(name);
            IFormField field = createField(name);
            field.getControl().setDisable(isAlwaysDisabled(name));

            fieldsMap.put(name, field);
            labelsMap.put(name, label);

            int col = (i % 2) * 2;
            row = i / 2;
            form.add(label, col, row);
            form.add(field.getControl(), col + 1, row);

            // Special logic for user subtypes
            if ("usuario".equals(tableName) && "tipo_usuario".equals(name)) {
                field.addListener(this::handleTipoUsuarioChange);
            }
        }

        // Add extra fields for usuario subtypes (hidden by default)
        if ("usuario".equals(tableName)) {
            addUsuarioSubtypeFields(row + 1);
        }
    }

    private void setupColumns() {
        for (int i = 0; i < 2; i++) {
            ColumnConstraints labelCol = new ColumnConstraints();
            labelCol.setMinWidth(110);
            labelCol.setHalignment(javafx.geometry.HPos.RIGHT);
            ColumnConstraints fieldCol = new ColumnConstraints();
            fieldCol.setHgrow(Priority.ALWAYS);
            fieldCol.setMinWidth(150);
            form.getColumnConstraints().addAll(labelCol, fieldCol);
        }
    }

    private Label createLabel(String name) {
        Label label = new Label(name.toUpperCase() + ":");
        label.getStyleClass().add("form-label");
        if (MetadataDAO.isColumnMandatory(tableName, name)) {
            label.setText(label.getText() + " *");
            label.getStyleClass().add("form-label-mandatory");
        }
        return label;
    }

    private void addUsuarioSubtypeFields(int startRow) {
        // We use a fixed layout for extra fields to avoid overlapping
        addExtraField("telefono_guardia", "Administrador", startRow, 0, true);
        addExtraField("direccion", "Normal", startRow, 0, false);
        addExtraField("telefono_movil", "Normal", startRow, 2, false);
        addExtraField("fotografia", "Normal", startRow + 1, 0, false);
    }

    private void addExtraField(
        String name,
        String type,
        int row,
        int col,
        boolean mandatory
    ) {
        Label label = new Label(name.toUpperCase() + ":");
        label.getStyleClass().add("form-label");
        label.setVisible(false);
        label.setManaged(false);
        label.setUserData(type);

        if (mandatory) {
            label.setText(label.getText() + " *");
            label.getStyleClass().add("form-label-mandatory");
            label.getProperties().put("mandatory", true); // Store mandatory flag
        }

        IFormField field = new TextFormField();
        field.getControl().setVisible(false);
        field.getControl().setManaged(false);
        field.getControl().setUserData(type);

        fieldsMap.put(name, field);
        labelsMap.put(name, label);

        form.add(label, col, row);
        form.add(field.getControl(), col + 1, row);
    }

    private void handleTipoUsuarioChange(Object newValue) {
        String type = newValue != null ? newValue.toString() : "";
        fieldsMap.forEach((name, field) -> {
            Object fieldType = field.getControl().getUserData();
            if (fieldType != null) {
                boolean visible = fieldType.equals(type);
                field.getControl().setVisible(visible);
                field.getControl().setManaged(visible);

                Label label = labelsMap.get(name);
                if (label != null) {
                    label.setVisible(visible);
                    label.setManaged(visible);
                }
            }
        });
    }

    private boolean isAlwaysDisabled(String name) {
        return "reserva".equals(tableName) && "id_reserva_local".equals(name);
    }

    private IFormField createField(String name) {
        if (foreignKeys.containsKey(name)) {
            ForeignKey fk = foreignKeys.get(name);
            return new DisplayComboFormField(
                MetadataDAO.getDisplayRowsFromTable(
                    fk.refTable(),
                    fk.refColumn()
                )
            );
        }
        List<String> allowedValues = MetadataDAO.getAllowedValues(
            tableName,
            name
        );
        if (allowedValues != null && !allowedValues.isEmpty()) {
            return new ComboFormField(allowedValues);
        }
        String columnType = MetadataDAO.getColumnType(tableName, name);
        if (columnType != null && columnType.equalsIgnoreCase("DATE")) {
            return new DateFormField();
        }
        return new TextFormField();
    }

    /**
     * Puts the form in edit mode after a table row is selected.
     * Primary keys are disabled because they identify the row being updated.
     */
    public void fill(List<String> values) {
        updateMode = true;
        for (int i = 0; i < columnNames.size(); i++) {
            String name = columnNames.get(i);
            IFormField field = fieldsMap.get(name);
            if (field != null) {
                // Never expose stored password hashes in the admin form.
                // Leaving it empty means "keep current password" on update.
                field.setValue(
                    "usuario".equals(tableName) && "contrasena".equals(name)
                        ? ""
                        : values.get(i)
                );
                field
                    .getControl()
                    .setDisable(
                        isAlwaysDisabled(name) ||
                            MetadataDAO.getPrimaryKeys(tableName).contains(name)
                    );
            }
        }
        loadUsuarioSubtypeValues(values);
    }

    /**
     * usuario is shown as one logical form, but subtype fields live in
     * administrador/usuarionormal. Load them after the base row is selected.
     */
    private void loadUsuarioSubtypeValues(List<String> values) {
        if (!"usuario".equals(tableName)) return;

        try {
            Object idUsuario = values.get(columnNames.indexOf("id_usuario"));
            String tipoUsuario = values.get(
                columnNames.indexOf("tipo_usuario")
            );
            Map<String, Object> subtypeValues =
                com.reservas.app.dao.UsuarioDAO.getSubtypeData(
                    idUsuario,
                    tipoUsuario
                );
            subtypeValues.forEach((name, value) -> {
                IFormField field = fieldsMap.get(name);
                if (field != null) field.setValue(
                    value != null ? value.toString() : ""
                );
            });
        } catch (Exception ignored) {
            // If subtype data cannot be loaded, the main usuario data is still usable.
        }
    }

    public void clear() {
        updateMode = false;
        fieldsMap.forEach((name, field) -> {
            field.clear();
            field.getControl().setDisable(isAlwaysDisabled(name));
        });
        if ("usuario".equals(tableName)) {
            handleTipoUsuarioChange(null);
        }
    }

    public Map<String, Object> getAllValues() {
        Map<String, Object> values = new HashMap<>();
        fieldsMap.forEach((name, field) -> {
            if (
                field.getControl().isManaged() &&
                !field.getControl().isDisabled()
            ) {
                values.put(name, field.getValue());
            }
        });
        return values;
    }

    public void validate() {
        fieldsMap.forEach((name, field) -> {
            if (field.getControl().isManaged()) {
                validateColumn(name, field.getValue());
            }
        });
    }

    private void validateColumn(String name, Object value) {
        boolean isMandatory = MetadataDAO.isColumnMandatory(tableName, name);

        // Check manually set mandatory flag for extra fields (user subtypes)
        Label label = labelsMap.get(name);
        if (
            label != null &&
            Boolean.TRUE.equals(label.getProperties().get("mandatory"))
        ) {
            isMandatory = true;
        }

        if (isMandatory && isRequiredInUI(name) && isValueEmpty(value)) {
            throw new IllegalArgumentException(
                "Field '" + name.toUpperCase() + "' is mandatory."
            );
        }

        if (!isValueEmpty(value)) {
            String type = MetadataDAO.getColumnType(tableName, name);
            if (type == null) return;

            validateByType(name, type, value.toString().trim());
        }
    }

    private boolean isRequiredInUI(String name) {
        if (
            "reserva".equals(tableName) && "id_reserva_local".equals(name)
        ) return false;
        return !(
            updateMode &&
            "usuario".equals(tableName) &&
            "contrasena".equals(name)
        );
    }

    private boolean isValueEmpty(Object value) {
        return value == null || value.toString().trim().isEmpty();
    }

    private void validateByType(String name, String type, String valStr) {
        String normalizedType = type.toUpperCase();
        if (normalizedType.startsWith("INTEGER")) validateInteger(name, valStr);
        else if (isNumericType(normalizedType)) validateNumeric(name, valStr);
        else if (normalizedType.startsWith("DATE")) validateDate(name, valStr);
        else if (name.startsWith("hora_")) validateTime(name, valStr);
    }

    private boolean isNumericType(String type) {
        return (
            type.startsWith("NUMERIC") ||
            type.startsWith("DECIMAL") ||
            type.startsWith("DOUBLE") ||
            type.startsWith("REAL") ||
            type.startsWith("FLOAT")
        );
    }

    private void validateInteger(String name, String valStr) {
        try {
            Integer.parseInt(valStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                "Field '" + name.toUpperCase() + "' must be a number."
            );
        }
    }

    private void validateNumeric(String name, String valStr) {
        try {
            Double.parseDouble(valStr);
            if (valStr.contains(".") && valStr.split("\\.")[1].length() > 2) {
                throw new IllegalArgumentException(
                    "Field '" + name.toUpperCase() + "' max 2 decimals."
                );
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                "Field '" + name.toUpperCase() + "' must be decimal."
            );
        }
    }

    private void validateDate(String name, String valStr) {
        java.time.LocalDate date;
        try {
            date = java.time.LocalDate.parse(valStr);
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException(
                "Field '" + name.toUpperCase() + "' must be YYYY-MM-DD."
            );
        }
        if (
            "reserva".equals(tableName) &&
            "fecha".equals(name) &&
            date.isBefore(java.time.LocalDate.now())
        ) {
            throw new IllegalArgumentException("Past dates not allowed.");
        }
    }

    private void validateTime(String name, String valStr) {
        try {
            if (valStr.length() == 5) valStr = valStr + ":00";
            java.time.LocalTime.parse(valStr);
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException(
                "Field '" + name.toUpperCase() + "' must be HH:mm or HH:mm:ss."
            );
        }
    }

    public void refreshCombos() {
        fieldsMap.forEach((columnName, field) -> {
            if (
                field instanceof DisplayComboFormField displayCombo &&
                foreignKeys.containsKey(columnName)
            ) {
                ForeignKey fk = foreignKeys.get(columnName);
                displayCombo.refreshItems(
                    MetadataDAO.getDisplayRowsFromTable(
                        fk.refTable(),
                        fk.refColumn()
                    )
                );
            } else if (field instanceof ComboFormField combo) {
                List<String> allowedValues = MetadataDAO.getAllowedValues(
                    tableName,
                    columnName
                );
                if (allowedValues != null) combo.refreshItems(allowedValues);
            }
        });
    }
}
