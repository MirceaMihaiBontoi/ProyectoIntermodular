package com.reservas.app.controller;

import java.io.IOException;
import java.util.logging.Logger;
import com.reservas.app.App;
import javafx.fxml.FXML;

public class PrimaryController {

    private static final Logger logger = Logger.getLogger(PrimaryController.class.getName());

    @FXML
    private void switchToSecondary() throws IOException {
        App.setRoot("user_management");
        logger.info("Switching to User Management screen...");
    }
}
