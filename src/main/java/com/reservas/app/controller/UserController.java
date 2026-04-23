package com.reservas.app.controller;

import com.reservas.app.App;
import com.reservas.app.dao.UserDAO;
import com.reservas.app.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserController {

    private static final Logger logger = Logger.getLogger(UserController.class.getName());

    @FXML
    private TableView<User> userTable;
    @FXML
    private TableColumn<User, Integer> colId;
    @FXML
    private TableColumn<User, String> colName;
    @FXML
    private TableColumn<User, String> colEmail;
    @FXML
    private TableColumn<User, String> colType;

    @FXML
    private TextField txtId;
    @FXML
    private TextField txtName;
    @FXML
    private TextField txtEmail;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private DatePicker dpBirthDate;
    @FXML
    private ComboBox<String> cbType;
    @FXML
    private TextArea txtSqlConsole;

    private UserDAO userDAO = new UserDAO();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colType.setCellValueFactory(new PropertyValueFactory<>("userType"));

        cbType.setItems(FXCollections.observableArrayList("Administrador", "Normal"));

        userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                fillForm(newSelection);
            }
        });

        refreshList();
    }

    private void fillForm(User user) {
        txtId.setText(String.valueOf(user.getId()));
        txtName.setText(user.getName());
        txtEmail.setText(user.getEmail());
        txtPassword.setText(user.getPassword());
        dpBirthDate.setValue(user.getBirthDate());
        cbType.setValue(user.getUserType());
    }

    @FXML
    private void clearFields() {
        txtId.clear();
        txtName.clear();
        txtEmail.clear();
        txtPassword.clear();
        dpBirthDate.setValue(null);
        cbType.setValue(null);
        userTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void refreshList() {
        List<User> users = userDAO.getAll();
        userTable.setItems(FXCollections.observableArrayList(users));
        logger.info("User list refreshed.");
    }

    // Validates that mandatory fields are not empty
    private boolean validateForm() {
        if (txtId.getText().isEmpty() || txtName.getText().isEmpty() || 
            txtEmail.getText().isEmpty() || cbType.getValue() == null) {
            
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation Error");
            alert.setHeaderText(null);
            alert.setContentText("Please fill in all mandatory fields (ID, Name, Email, and Type).");
            alert.showAndWait();
            return false;
        }
        return true;
    }

    @FXML
    private void onAddUser() {
        if (!validateForm()) return;

        try {
            User user = new User(
                Integer.parseInt(txtId.getText()),
                txtEmail.getText(),
                txtPassword.getText(),
                txtName.getText(),
                dpBirthDate.getValue(),
                cbType.getValue()
            );
            userDAO.save(user);
            refreshList();
            clearFields();
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, "Invalid ID format: {0}", txtId.getText());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error adding user", e);
        }
    }

    @FXML
    private void onUpdateUser() {
        if (!validateForm()) return;

        try {
            User user = new User(
                Integer.parseInt(txtId.getText()),
                txtEmail.getText(),
                txtPassword.getText(),
                txtName.getText(),
                dpBirthDate.getValue(),
                cbType.getValue()
            );
            userDAO.update(user);
            refreshList();
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, "Invalid ID format: {0}", txtId.getText());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error updating user", e);
        }
    }

    @FXML
    private void onDeleteUser() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser != null) {
            userDAO.delete(selectedUser.getId());
            refreshList();
            clearFields();
        }
    }

    @FXML
    private void onExecuteRawSql() {
        String sql = txtSqlConsole.getText();
        if (sql == null || sql.trim().isEmpty()) return;

        try {
            userDAO.executeRawSql(sql);
            refreshList();
            txtSqlConsole.clear();
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("SQL Execution");
            alert.setHeaderText(null);
            alert.setContentText("Command executed successfully.");
            alert.showAndWait();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "SQL Execution failed", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("SQL Error");
            alert.setHeaderText("Execution failed");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void switchToPrimary() throws IOException {
        App.setRoot("primary");
    }
}
