package com.reservas.app.util.fields;

import javafx.scene.control.Control;

/**
 * INTERFACE: This is a 'contract' that any form field must follow.
 * It allows the FormManager to treat any control (TextField, ComboBox, etc.)
 * as a generic IFormField without knowing its internal implementation.
 */
public interface IFormField {
    /**
     * @return The current value entered or selected by the user.
     */
    Object getValue();

    /**
     * Sets a value into the control (e.g., when clicking a table row).
     */
    void setValue(String value);

    /**
     * Resets the control to its empty state.
     */
    void clear();

    /**
     * @return The underlying JavaFX Control (TextField, ComboBox) to add to the UI.
     */
    Control getControl();
}
