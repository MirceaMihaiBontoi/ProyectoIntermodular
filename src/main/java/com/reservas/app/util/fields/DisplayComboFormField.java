package com.reservas.app.util.fields;

import java.util.LinkedHashMap;
import java.util.Map;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;

/**
 * ComboBox for foreign keys.
 *
 * The admin should see a readable label such as "1 - Sala de reuniones A",
 * but the DAO must receive only the real FK value, for example "1".
 */
public class DisplayComboFormField implements IFormField {

    private final ComboBox<Option> comboBox = new ComboBox<>();

    public DisplayComboFormField(Map<String, String> items) {
        refreshItems(items);
        comboBox.setMaxWidth(Double.MAX_VALUE);
    }

    @Override
    public Object getValue() {
        Option selected = comboBox.getValue();
        // Persist the FK value, not the human-readable label shown in the UI.
        return selected != null ? selected.value() : null;
    }

    @Override
    public void setValue(String value) {
        if (value == null) {
            comboBox.setValue(null);
            return;
        }
        comboBox
            .getItems()
            .stream()
            .filter(option -> option.value().equals(value))
            .findFirst()
            .ifPresentOrElse(comboBox::setValue, () ->
                comboBox.setValue(new Option(value, value))
            );
    }

    @Override
    public void clear() {
        comboBox.getSelectionModel().clearSelection();
    }

    @Override
    public Control getControl() {
        return comboBox;
    }

    @Override
    public void addListener(java.util.function.Consumer<Object> listener) {
        comboBox
            .valueProperty()
            .addListener((obs, oldVal, newVal) ->
                listener.accept(newVal != null ? newVal.value() : null)
            );
    }

    public void refreshItems(Map<String, String> newItems) {
        // Keep the selected FK after another tab changes reference data.
        Object currentValue = getValue();
        comboBox.getItems().clear();
        new LinkedHashMap<>(newItems).forEach((value, label) ->
            comboBox.getItems().add(new Option(value, label))
        );
        if (currentValue != null) setValue(currentValue.toString());
    }

    private record Option(String value, String label) {
        @Override
        public String toString() {
            return label;
        }
    }
}
