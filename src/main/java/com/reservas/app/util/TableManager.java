package com.reservas.app.util;

import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;
import java.util.function.Consumer;

public class TableManager {

    private final TableView<ObservableList<StringProperty>> table;
    private final List<String> columnNames;

    public TableManager(TableView<ObservableList<StringProperty>> table, List<String> columnNames) {
        this.table = table;
        this.columnNames = columnNames;
    }

    /**
     * Builds the table columns and sets up the selection listener.
     * @param onSelection A callback function that triggers when the user clicks a row.
     */
    public void build(Consumer<List<String>> onSelection) {
        // Clear old columns (important when switching between different database tables)
        table.getColumns().clear();

        for (int i = 0; i < columnNames.size(); i++) {
            // We need a 'final' variable because it's used inside a Lambda expression below
            final int index = i;
            
            // UI Polish: Make headers look nicer (e.g. "id_usuario" -> "ID USUARIO")
            String headerText = columnNames.get(index).toUpperCase().replace("_", " ");
            
            TableColumn<ObservableList<StringProperty>, String> column = new TableColumn<>(headerText);
            
            /**
             * CELL VALUE FACTORY:
             * This is the logic that connects a table cell to the data source.
             * It tells JavaFX: "For each row in this column, go to the list and 
             * pick the item at position 'index'".
             */
            column.setCellValueFactory(data -> data.getValue().get(index));
            
            column.setMinWidth(120);
            column.setStyle("-fx-alignment: CENTER;"); // Center the text in the table
            table.getColumns().add(column);
        }

        /**
         * SELECTION LISTENER:
         * This "listens" to when the user clicks on a row in the table.
         * When a click happens, we extract the data from that row and pass it 
         * back to the controller so it can fill the form.
         */
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null && onSelection != null) {
                // Convert ObservableList<StringProperty> back to a simple List<String>
                onSelection.accept(newSelection.stream()
                        .map(StringProperty::get)
                        .toList());
            }
        });
    }
}
