package dsounds.controllers;

import dsounds.App;
import dsounds.models.User;
import dsounds.security.RoleGuard;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.io.IOException;

public class GlobalBarController {

    @FXML
    private Label connectedUserLabel;

    @FXML
    private Label nowPlayingLabel;

    @FXML
    private Button playPauseButton;

    @FXML
    private Button playlistsButton;

    @FXML
    private Button uploadButton;

    @FXML
    private Button usersButton;

    @FXML
    private Button statsButton;

    private Timeline refreshTimeline;

    @FXML
    private void initialize() {
        User currentUser = App.getAuthController().getSession().getCurrentUser();
        connectedUserLabel.setText("Connected: " + currentUser.getUsername() + " (" + currentUser.getRole() + ")");

        boolean canPlaylists = RoleGuard.canCreatePlaylist(App.getAuthController().getSession());
        boolean canCatalog = RoleGuard.canManageCatalog(App.getAuthController().getSession());
        boolean canUsers = RoleGuard.canManageUsers(App.getAuthController().getSession());

        playlistsButton.setVisible(canPlaylists);
        playlistsButton.setManaged(canPlaylists);

        uploadButton.setVisible(canCatalog);
        uploadButton.setManaged(canCatalog);

        usersButton.setVisible(canUsers);
        usersButton.setManaged(canUsers);

        statsButton.setVisible(canUsers);
        statsButton.setManaged(canUsers);

        refreshNowPlaying();
        refreshTimeline = new Timeline(new KeyFrame(Duration.millis(350), e -> refreshNowPlaying()));
        refreshTimeline.setCycleCount(Animation.INDEFINITE);
        refreshTimeline.play();
    }

    private void refreshNowPlaying() {
        String title = App.getNowPlayingTitle();
        String artist = App.getNowPlayingArtist();
        if (title == null || title.isBlank()) {
            nowPlayingLabel.setText("Now playing: nothing");
        } else {
            String artistText = (artist == null || artist.isBlank()) ? "Unknown artist" : artist;
            nowPlayingLabel.setText("Now playing: " + title + " - " + artistText);
        }

        playPauseButton.setDisable(!App.hasGlobalPlayer());
        playPauseButton.setText(App.isGlobalPaused() ? "Play" : "Pause");
    }

    @FXML
    private void goHome() throws IOException {
        App.setRoot("browser");
    }

    @FXML
    private void goPlaylists() throws IOException {
        if (!RoleGuard.canCreatePlaylist(App.getAuthController().getSession())) {
            return;
        }
        App.setRoot("playlist");
    }

    @FXML
    private void goUpload() throws IOException {
        if (!RoleGuard.canManageCatalog(App.getAuthController().getSession())) {
            return;
        }
        App.setRoot("artist");
    }

    @FXML
    private void goUsers() throws IOException {
        if (!RoleGuard.canManageUsers(App.getAuthController().getSession())) {
            return;
        }
        App.setRoot("admin_users");
    }

    @FXML
    private void goStats() throws IOException {
        if (!RoleGuard.canManageUsers(App.getAuthController().getSession())) {
            return;
        }
        App.setRoot("admin_stats");
    }

    @FXML
    private void previousTrack() {
        App.playPreviousGlobal();
        refreshNowPlaying();
    }

    @FXML
    private void togglePlayPause() {
        App.toggleGlobalPause();
        refreshNowPlaying();
    }

    @FXML
    private void nextTrack() {
        App.playNextGlobal();
        refreshNowPlaying();
    }

    @FXML
    private void logout() throws IOException {
        App.stopGlobalPlayer();
        App.getAuthController().logout();
        App.resetVisitorPlayCount();
        App.setRoot("auth");
    }
}
