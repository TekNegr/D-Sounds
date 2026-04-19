package dsounds.controllers;

import dsounds.App;
import dsounds.models.User;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * AuthController handles user authentication and user management.
 */
public class AuthController {

    @FXML
    private TextField loginUsernameField;

    @FXML
    private PasswordField loginPasswordField;

    @FXML
    private TextField registerUsernameField;

    @FXML
    private TextField registerEmailField;

    @FXML
    private PasswordField registerPasswordField;

    @FXML
    private Label statusLabel;

    @FXML
    private void initialize() {
        statusLabel.setText("Use admin / root to connect as administrator.");
    }

    @FXML
    private void login() {
        try {
            User user = App.getAuthController().login(
                    loginUsernameField.getText(),
                    loginPasswordField.getText()
            );
            statusLabel.setText("Welcome, " + user.getUsername() + " (" + user.getRole() + ")");
            App.setRoot("dashboard");
        } catch (AuthException | IOException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void register() {
        try {
            User user = App.getAuthController().registerSubscriber(
                    registerUsernameField.getText(),
                    registerEmailField.getText(),
                    registerPasswordField.getText()
            );
            statusLabel.setText("Account created for " + user.getUsername() + ". You can now log in.");
            registerPasswordField.clear();
        } catch (AuthException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void continueAsVisitor() {
        try {
            App.getAuthController().continueAsVisitor();
            statusLabel.setText("Connected as visitor.");
            App.setRoot("dashboard");
        } catch (IOException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }
}
