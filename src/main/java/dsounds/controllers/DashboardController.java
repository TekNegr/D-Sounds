package dsounds.controllers;

import dsounds.App;
import dsounds.models.AuthSession;
import dsounds.models.User;
import dsounds.models.UserRole;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class DashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;
    @FXML private Label visitorInfoLabel;

    @FXML private Button btnArtist;
    @FXML private Button btnLibrary;
    @FXML private Button btnPlaylists;
    @FXML private Button btnManageUsers;
    @FXML private Button btnStats;

    @FXML
    private void initialize() {
        AuthSession session = App.getAuthController().getSession();
        User user = session.getCurrentUser();
        UserRole role = user.getRole();

        welcomeLabel.setText("Welcome, " + user.getDisplayName());
        roleLabel.setText("Connected as: " + role.name());

        btnArtist.setVisible(false);
        btnArtist.setManaged(false);
        btnLibrary.setVisible(false);
        btnLibrary.setManaged(false);
        btnPlaylists.setVisible(false);
        btnPlaylists.setManaged(false);
        btnManageUsers.setVisible(false);
        btnManageUsers.setManaged(false);
        btnStats.setVisible(false);
        btnStats.setManaged(false);
        visitorInfoLabel.setVisible(false);
        visitorInfoLabel.setManaged(false);

        switch (role) {
            case ADMIN:
                btnArtist.setVisible(true);
                btnArtist.setManaged(true);
                btnLibrary.setVisible(true);
                btnLibrary.setManaged(true);
                btnPlaylists.setVisible(true);
                btnPlaylists.setManaged(true);
                btnManageUsers.setVisible(true);
                btnManageUsers.setManaged(true);
                btnStats.setVisible(true);
                btnStats.setManaged(true);
                break;

            case SUBSCRIBER:
                btnArtist.setVisible(true);
                btnArtist.setManaged(true);
                btnLibrary.setVisible(true);
                btnLibrary.setManaged(true);
                btnPlaylists.setVisible(true);
                btnPlaylists.setManaged(true);
                break;

            case VISITOR:
                visitorInfoLabel.setVisible(true);
                visitorInfoLabel.setManaged(true);
                visitorInfoLabel.setText("Visitor mode: limited to 5 listens, no playlists.");
                break;
        }
    }

    @FXML
    private void switchToBrowser() throws IOException {
        App.setRoot("browser");
    }

    @FXML
    private void switchToArtist() throws IOException {
        App.setRoot("artist");
    }

    @FXML
    private void switchToList() throws IOException {
        App.setRoot("list");
    }

    @FXML
    private void switchToPlaylists() throws IOException {
        App.setRoot("playlist");
    }

    @FXML
    private void switchToManageUsers() throws IOException {
        App.setRoot("admin_users");
    }

    @FXML
    private void switchToStats() throws IOException {
        App.setRoot("admin_stats");
    }

    @FXML
    private void logout() throws IOException {
        App.getAuthController().logout();
        App.setRoot("auth");
    }
}