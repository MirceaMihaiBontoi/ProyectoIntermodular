package com.reservas.app.util.fields;

import javafx.scene.control.Control;
import javafx.scene.control.TextField;

/**
 * Implementation of IFormField for standard text input.
 * 
 * ENCAPSULATION:
 * Notice how this class "hides" the actual TextField. 
 * The rest of the app only knows it's an 'IFormField'. This makes it easy
 * to change the UI library later without breaking the logic.
 */
public class TextFormField implements IFormField {
    // We keep the actual control private (Encapsulation)
    private final TextField textField;

    public TextFormField() {
        this.textField = new TextField();
        // Allow the text field to grow horizontally
        this.textField.setMaxWidth(Double.MAX_VALUE);
    }

    @Override
    public Object getValue() {
        return textField.getText();
    }

    @Override
    public void setValue(String value) {
        textField.setText(value != null ? value : "");
    }

    /**
     * Clears the text from the field.
     */
    @Override
    public void clear() {
        textField.clear();
    }

    /**
     * Returns the actual JavaFX Control for the layout.
     */
    @Override
    public Control getControl() {
        return textField;
    }
}
