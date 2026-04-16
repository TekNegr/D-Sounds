package dsounds.controllers;

import dsounds.App;

import java.io.IOException;

import javafx.fxml.FXML;

public class DashboardController {

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
    private void logout() throws IOException {
        App.getAuthController().logout();
        App.setRoot("auth");
    }
}
