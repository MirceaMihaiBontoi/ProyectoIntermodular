package com.reservas.app.util.fields;

import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import java.util.List;

/**
 * Implementation of IFormField for dropdown selections.
 * 
 * By using a ComboBox instead of a TextField, we ensure the user can only 
 * select valid, existing records, preventing database errors.
 */
public class ComboFormField implements IFormField {
    private final ComboBox<String> comboBox;

    public ComboFormField(List<String> items) {
        this.comboBox = new ComboBox<>();
        this.comboBox.getItems().addAll(items);
        // Make the dropdown expand to fill the full width of its container
        this.comboBox.setMaxWidth(Double.MAX_VALUE);
    }

    @Override
    public Object getValue() {
        return comboBox.getValue();
    }

    @Override
    public void setValue(String value) {
        comboBox.setValue(value);
    }

    /**
     * Resets the selection.
     */
    @Override
    public void clear() {
        comboBox.getSelectionModel().clearSelection();
    }

    /**
     * Returns the actual JavaFX Control to be displayed in the UI.
     */
    @Override
    public Control getControl() {
        return comboBox;
    }
}
