package com.reservas.app.util.fields;

import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Implementation of IFormField for date input using DatePicker.
 * Ensures proper date format and prevents invalid text input.
 */
public class DateFormField implements IFormField {
    private final DatePicker datePicker;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public DateFormField() {
        this.datePicker = new DatePicker();
        this.datePicker.setMaxWidth(Double.MAX_VALUE);
    }

    @Override
    public Object getValue() {
        LocalDate date = datePicker.getValue();
        return date != null ? date.format(DATE_FORMATTER) : null;
    }

    @Override
    public void setValue(String value) {
        if (value != null && !value.isEmpty()) {
            try {
                LocalDate date = LocalDate.parse(value, DATE_FORMATTER);
                datePicker.setValue(date);
            } catch (Exception e) {
                // If parsing fails, just clear the date
                datePicker.setValue(null);
            }
        } else {
            datePicker.setValue(null);
        }
    }

    @Override
    public void clear() {
        datePicker.setValue(null);
    }

    @Override
    public Control getControl() {
        return datePicker;
    }
}
