package dsounds.controllers;

import dsounds.App;
import dsounds.models.Song;
import dsounds.models.User;
import dsounds.models.UserRole;
import dsounds.repositories.AlbumRepository;
import dsounds.repositories.PlaylistRepository;
import dsounds.repositories.SongRepository;

import java.io.IOException;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class AdminStatsController {

    @FXML private Label totalUsersLabel;
    @FXML private Label subscribersLabel;
    @FXML private Label adminsLabel;
    @FXML private Label suspendedLabel;

    @FXML private Label totalSongsLabel;
    @FXML private Label totalAlbumsLabel;
    @FXML private Label totalPlaylistsLabel;
    @FXML private Label uniqueArtistsLabel;
    @FXML private Label uniqueGenresLabel;

    @FXML
    private void initialize() {
        refresh();
    }

    @FXML
    private void refresh() {
        loadUserStats();
        loadCatalogueStats();
    }

    private void loadUserStats() {
        List<User> users = App.getAuthController().listUsers();

        long subscribers = users.stream()
                .filter(u -> u.getRole() == UserRole.SUBSCRIBER)
                .count();
        long admins = users.stream()
                .filter(u -> u.getRole() == UserRole.ADMIN)
                .count();
        long suspended = users.stream()
                .filter(u -> !u.isActive())
                .count();

        totalUsersLabel.setText(String.valueOf(users.size()));
        subscribersLabel.setText(String.valueOf(subscribers));
        adminsLabel.setText(String.valueOf(admins));
        suspendedLabel.setText(String.valueOf(suspended));
    }

    private void loadCatalogueStats() {
        try {
            List<Song> songs = SongRepository.loadAllLocalSongs();
            totalSongsLabel.setText(String.valueOf(songs.size()));

            long uniqueArtists = songs.stream()
                    .map(Song::getArtist)
                    .filter(a -> a != null && !a.isBlank())
                    .distinct()
                    .count();
            uniqueArtistsLabel.setText(String.valueOf(uniqueArtists));

            long uniqueGenres = songs.stream()
                    .map(Song::getGenre)
                    .filter(g -> g != null && !g.isBlank())
                    .distinct()
                    .count();
            uniqueGenresLabel.setText(String.valueOf(uniqueGenres));

        } catch (IOException ex) {
            totalSongsLabel.setText("Error");
            uniqueArtistsLabel.setText("Error");
            uniqueGenresLabel.setText("Error");
        }

        try {
            int albumCount = AlbumRepository.loadAllAlbums().size();
            totalAlbumsLabel.setText(String.valueOf(albumCount));
        } catch (IOException ex) {
            totalAlbumsLabel.setText("Error");
        }

        try {
            int playlistCount = PlaylistRepository.loadAllPlaylists().size();
            totalPlaylistsLabel.setText(String.valueOf(playlistCount));
        } catch (IOException ex) {
            totalPlaylistsLabel.setText("Error");
        }
    }

    @FXML
    private void backToDashboard() throws IOException {
        App.setRoot("browser");
    }
}