package com.reservas.app.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Utility class for showing UI dialogs and handling common database action logic.
 */
public class DialogHelper {

    private DialogHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static void showInfo(String title, String content) {
        showAlert(Alert.AlertType.INFORMATION, title, content);
    }

    public static void showError(String title, String content) {
        showAlert(Alert.AlertType.ERROR, title, content);
    }

    public static boolean showConfirmation(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private static void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * HIGH-LEVEL ABSTRACTION: This method takes a 'DbAction' (a block of code)
     * and runs it inside a try-catch. This prevents duplicating error handling 
     * code in every controller.
     * 
     * @param action The code to execute (usually a DAO call).
     * @param successMsg Message to show if it works.
     * @param onSuccess Extra code to run after success (like refreshing the UI).
     */
    public static void doDbAction(DbAction action, String successMsg, Runnable onSuccess) {
        try {
            // Run the lambda block
            action.execute();
            if (successMsg != null) showInfo("Success", successMsg);
            if (onSuccess != null) onSuccess.run();
        } catch (SQLException e) {
            // Centralized error handling
            showError("Database Error", e.getMessage());
        }
    }

    /**
     * Functional Interface: This allows us to pass blocks of code that 
     * throw SQLException as arguments.
     */
    @FunctionalInterface
    public interface DbAction {
        void execute() throws SQLException;
    }
}
