package dsounds.controllers;

import dsounds.App;
import dsounds.models.Playlist;
import dsounds.models.Song;
import dsounds.models.User;
import dsounds.models.UserRole;
import dsounds.repositories.PlaylistRepository;
import dsounds.repositories.SongRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Collections;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.input.MouseButton;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * PlaylistController handles playlist creation, management, and song operations.
 */
public class PlaylistController {

    private enum RepeatMode {
        NONE,
        PLAYLIST,
        SONG
    }

    @FXML
    private TextField playlistNameField;

    @FXML
    private TextField searchField;

    @FXML
    private CheckBox publicPlaylistCheckBox;

    @FXML
    private TextArea playlistDescriptionArea;

    @FXML
    private ListView<Playlist> playlistListView;

    @FXML
    private ListView<Song> availableSongsListView;

    @FXML
    private ListView<Song> playlistSongsListView;

    @FXML
    private Label statusLabel;

    @FXML
    private Button createPlaylistButton;

    @FXML
    private Button updatePlaylistButton;

    @FXML
    private Button deletePlaylistButton;

    @FXML
    private Button addSongButton;

    @FXML
    private Button removeSongButton;

    @FXML
    private Button playInOrderButton;

    @FXML
    private Button playRandomButton;

    @FXML
    private Button stopPlaybackButton;

    @FXML
    private Button repeatModeButton;

    @FXML
    private Button clearSearchButton;

    private final ObservableList<Playlist> playlists = FXCollections.observableArrayList();
    private final ObservableList<Song> availableSongs = FXCollections.observableArrayList();
    private final ObservableList<Song> playlistSongs = FXCollections.observableArrayList();
    private final List<Song> allSongs = new ArrayList<>();
    private final List<Song> currentPlaylistAllSongs = new ArrayList<>();
    private final Map<String, Song> songsById = new HashMap<>();
    private final Random random = new Random();
    private MediaPlayer mediaPlayer;
    private final List<Song> playbackQueue = new ArrayList<>();
    private int playbackIndex;
    private RepeatMode repeatMode = RepeatMode.NONE;

    @FXML
    private void initialize() {
        playlistListView.setItems(playlists);
        availableSongsListView.setItems(availableSongs);
        playlistSongsListView.setItems(playlistSongs);

        availableSongsListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Song item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getSummary());
            }
        });

        playlistSongsListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Song item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getSummary());
            }
        });

        availableSongsListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                goToLibraryWithSelectedSong(availableSongsListView.getSelectionModel().getSelectedItem());
            }
        });

        playlistSongsListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                goToLibraryWithSelectedSong(playlistSongsListView.getSelectionModel().getSelectedItem());
            }
        });

        playlistListView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            stopPlaybackInternal();
            fillPlaylistForm(newValue);
            loadPlaylistSongs(newValue);
            updatePermissions(newValue);
        });

        searchField.textProperty().addListener((observable, oldValue, newValue) -> applySongSearch());

        refreshAll();
    }

    @FXML
    private void clearSearch() {
        searchField.clear();
        applySongSearch();
    }

    @FXML
    private void refreshAll() {
        try {
            List<Song> allSongs = SongRepository.loadAllLocalSongs();
            this.allSongs.clear();
            this.allSongs.addAll(allSongs);
            songsById.clear();
            for (Song song : allSongs) {
                songsById.put(song.getId(), song);
            }

            List<Playlist> loadedPlaylists = PlaylistRepository.loadAllPlaylists();
            playlists.setAll(filterVisiblePlaylists(loadedPlaylists));
            statusLabel.setText("Playlists loaded: " + playlists.size());

            if (!playlists.isEmpty() && playlistListView.getSelectionModel().getSelectedItem() == null) {
                playlistListView.getSelectionModel().selectFirst();
            }

            applySongSearch();
            updatePermissions(playlistListView.getSelectionModel().getSelectedItem());
        } catch (IOException ex) {
            statusLabel.setText("Failed to refresh playlists: " + ex.getMessage());
        }
    }

    @FXML
    private void createPlaylist() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            statusLabel.setText("You must be logged in.");
            return;
        }
        if (currentUser.getRole() == UserRole.VISITOR) {
            statusLabel.setText("Visitors cannot create playlists. Please log in.");
            return;
        }

        String playlistName = safeTrim(playlistNameField.getText());
        if (playlistName.isEmpty()) {
            statusLabel.setText("Playlist name is required.");
            return;
        }

        Playlist playlist = new Playlist(currentUser.getUsername(), playlistName);
        playlist.setUserId(currentUser.getId());
        playlist.setDescription(safeTrim(playlistDescriptionArea.getText()));
        playlist.setVisibility(publicPlaylistCheckBox.isSelected() ? Playlist.Visibility.PUBLIC : Playlist.Visibility.PRIVATE);

        try {
            PlaylistRepository.savePlaylist(playlist);
            refreshAll();
            selectPlaylistById(playlist.getId());
            statusLabel.setText("Playlist created.");
        } catch (IOException ex) {
            statusLabel.setText("Could not create playlist: " + ex.getMessage());
        }
    }

    @FXML
    private void updatePlaylist() {
        Playlist selectedPlaylist = playlistListView.getSelectionModel().getSelectedItem();
        if (selectedPlaylist == null) {
            statusLabel.setText("Select a playlist first.");
            return;
        }

        if (!canEdit(selectedPlaylist)) {
            statusLabel.setText("Only admins or playlist owners can update this playlist.");
            return;
        }

        String playlistName = safeTrim(playlistNameField.getText());
        if (playlistName.isEmpty()) {
            statusLabel.setText("Playlist name is required.");
            return;
        }

        selectedPlaylist.setName(playlistName);
        selectedPlaylist.setDescription(safeTrim(playlistDescriptionArea.getText()));
        selectedPlaylist.setVisibility(publicPlaylistCheckBox.isSelected() ? Playlist.Visibility.PUBLIC : Playlist.Visibility.PRIVATE);

        try {
            PlaylistRepository.savePlaylist(selectedPlaylist);
            refreshAll();
            selectPlaylistById(selectedPlaylist.getId());
            statusLabel.setText("Playlist updated.");
        } catch (IOException ex) {
            statusLabel.setText("Could not update playlist: " + ex.getMessage());
        }
    }

    @FXML
    private void deletePlaylist() {
        Playlist selectedPlaylist = playlistListView.getSelectionModel().getSelectedItem();
        if (selectedPlaylist == null) {
            statusLabel.setText("Select a playlist first.");
            return;
        }

        if (!canEdit(selectedPlaylist)) {
            statusLabel.setText("Only admins or playlist owners can delete this playlist.");
            return;
        }

        try {
            PlaylistRepository.deletePlaylist(selectedPlaylist.getId());
            refreshAll();
            clearPlaylistForm();
            statusLabel.setText("Playlist deleted.");
        } catch (IOException ex) {
            statusLabel.setText("Could not delete playlist: " + ex.getMessage());
        }
    }

    @FXML
    private void addSelectedSongToPlaylist() {
        Playlist selectedPlaylist = playlistListView.getSelectionModel().getSelectedItem();
        Song selectedSong = availableSongsListView.getSelectionModel().getSelectedItem();

        if (selectedPlaylist == null || selectedSong == null) {
            statusLabel.setText("Select both a playlist and a song.");
            return;
        }

        if (!canEdit(selectedPlaylist)) {
            statusLabel.setText("Only admins or playlist owners can modify this playlist.");
            return;
        }

        selectedPlaylist.addSong(selectedSong.getId());
        try {
            PlaylistRepository.savePlaylist(selectedPlaylist);
            loadPlaylistSongs(selectedPlaylist);
            statusLabel.setText("Song added to playlist.");
        } catch (IOException ex) {
            statusLabel.setText("Could not add song: " + ex.getMessage());
        }
    }

    @FXML
    private void removeSelectedSongFromPlaylist() {
        Playlist selectedPlaylist = playlistListView.getSelectionModel().getSelectedItem();
        Song selectedSong = playlistSongsListView.getSelectionModel().getSelectedItem();

        if (selectedPlaylist == null || selectedSong == null) {
            statusLabel.setText("Select both a playlist and a playlist song.");
            return;
        }

        if (!canEdit(selectedPlaylist)) {
            statusLabel.setText("Only admins or playlist owners can modify this playlist.");
            return;
        }

        selectedPlaylist.removeSong(selectedSong.getId());
        try {
            PlaylistRepository.savePlaylist(selectedPlaylist);
            loadPlaylistSongs(selectedPlaylist);
            statusLabel.setText("Song removed from playlist.");
        } catch (IOException ex) {
            statusLabel.setText("Could not remove song: " + ex.getMessage());
        }
    }

    @FXML
    private void switchToDashboard() throws IOException {
        stopPlaybackInternal();
        App.setRoot("dashboard");
    }

    @FXML
    private void playPlaylistInOrder() {
        Playlist selectedPlaylist = playlistListView.getSelectionModel().getSelectedItem();
        if (selectedPlaylist == null) {
            statusLabel.setText("Select a playlist first.");
            return;
        }

        List<Song> songs = playlistSongs.stream().collect(Collectors.toList());
        if (songs.isEmpty()) {
            statusLabel.setText("Playlist is empty.");
            return;
        }

        playbackQueue.clear();
        playbackQueue.addAll(songs);
        playbackIndex = 0;
        playCurrentQueueSong();
    }

    @FXML
    private void playPlaylistRandom() {
        Playlist selectedPlaylist = playlistListView.getSelectionModel().getSelectedItem();
        if (selectedPlaylist == null) {
            statusLabel.setText("Select a playlist first.");
            return;
        }

        List<Song> songs = playlistSongs.stream().collect(Collectors.toList());
        if (songs.isEmpty()) {
            statusLabel.setText("Playlist is empty.");
            return;
        }

        playbackQueue.clear();
        playbackQueue.addAll(songs);
        Collections.shuffle(playbackQueue, random);
        playbackIndex = 0;
        playCurrentQueueSong();
    }

    @FXML
    private void stopPlayback() {
        stopPlaybackInternal();
        statusLabel.setText("Playlist playback stopped.");
    }

    @FXML
    private void toggleRepeatMode() {
        if (repeatMode == RepeatMode.NONE) {
            repeatMode = RepeatMode.PLAYLIST;
        } else if (repeatMode == RepeatMode.PLAYLIST) {
            repeatMode = RepeatMode.SONG;
        } else {
            repeatMode = RepeatMode.NONE;
        }

        updateRepeatButtonText();
        statusLabel.setText("Repeat mode: " + repeatModeToLabel(repeatMode));
    }

    private List<Playlist> filterVisiblePlaylists(List<Playlist> allPlaylists) {
        User currentUser = getCurrentUser();
        boolean isAdmin = currentUser != null && currentUser.getRole() == UserRole.ADMIN;
        String username = currentUser == null ? "" : currentUser.getUsername();

        return allPlaylists.stream()
                .filter(playlist -> playlist.isPublic()
                        || isAdmin
                        || (playlist.getOwnerUsername() != null
                            && playlist.getOwnerUsername().equalsIgnoreCase(username)))
                .collect(Collectors.toList());
    }

    private void fillPlaylistForm(Playlist playlist) {
        if (playlist == null) {
            clearPlaylistForm();
            return;
        }

        playlistNameField.setText(playlist.getName());
        playlistDescriptionArea.setText(playlist.getDescription());
        publicPlaylistCheckBox.setSelected(playlist.isPublic());
    }

    private void clearPlaylistForm() {
        playlistNameField.clear();
        playlistDescriptionArea.clear();
        publicPlaylistCheckBox.setSelected(true);
        playlistSongs.clear();
        currentPlaylistAllSongs.clear();
        applySongSearch();
    }

    private void loadPlaylistSongs(Playlist playlist) {
        if (playlist == null) {
            currentPlaylistAllSongs.clear();
            applySongSearch();
            return;
        }

        currentPlaylistAllSongs.clear();
        for (String songId : playlist.getSongIds()) {
            Song song = songsById.get(songId);
            if (song != null) {
                currentPlaylistAllSongs.add(song);
            }
        }
        applySongSearch();
    }

    private boolean canEdit(Playlist playlist) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return false;
        }
        if (currentUser.getRole() == UserRole.VISITOR) {
            return false;
        }

        if (currentUser.getRole() == UserRole.ADMIN) {
            return true;
        }

        return playlist.getOwnerUsername() != null
                && playlist.getOwnerUsername().equalsIgnoreCase(currentUser.getUsername());
    }

    private void selectPlaylistById(String playlistId) {
        for (Playlist playlist : playlists) {
            if (playlist.getId().equals(playlistId)) {
                playlistListView.getSelectionModel().select(playlist);
                return;
            }
        }
    }

    private User getCurrentUser() {
        return App.getAuthController().getSession().getCurrentUser();
    }

    private void applySongSearch() {
        String query = searchField.getText();

        List<Song> filteredAvailableSongs = allSongs.stream()
                .filter(song -> SongSearchUtils.matches(song, query))
                .collect(Collectors.toList());
        availableSongs.setAll(filteredAvailableSongs);

        List<Song> filteredPlaylistSongs = currentPlaylistAllSongs.stream()
                .filter(song -> SongSearchUtils.matches(song, query))
                .collect(Collectors.toList());
        playlistSongs.setAll(filteredPlaylistSongs);

        if (clearSearchButton != null) {
            clearSearchButton.setDisable(query == null || query.isBlank());
        }
    }

    private void updatePermissions(Playlist selectedPlaylist) {
        User currentUser = getCurrentUser();
        boolean canCreate = currentUser != null && currentUser.getRole() != UserRole.VISITOR;
        boolean editable = selectedPlaylist != null && canEdit(selectedPlaylist);
        boolean hasSongsToPlay = selectedPlaylist != null && !playlistSongs.isEmpty();

        createPlaylistButton.setDisable(!canCreate);
        updatePlaylistButton.setDisable(!editable);
        deletePlaylistButton.setDisable(!editable);
        addSongButton.setDisable(!editable);
        removeSongButton.setDisable(!editable);
        playInOrderButton.setDisable(!hasSongsToPlay);
        playRandomButton.setDisable(!hasSongsToPlay);
        stopPlaybackButton.setDisable(mediaPlayer == null);
        repeatModeButton.setDisable(!hasSongsToPlay && mediaPlayer == null);
        updateRepeatButtonText();
    }

    private void playCurrentQueueSong() {
        if (playbackIndex < 0 || playbackIndex >= playbackQueue.size()) {
            stopPlaybackInternal();
            statusLabel.setText("Playlist playback finished.");
            return;
        }

        Song currentSong = playbackQueue.get(playbackIndex);
        Path path = resolveSongPath(currentSong);
        if (path == null || !Files.exists(path)) {
            statusLabel.setText("Missing audio file for: " + currentSong.getTitle());
            playbackIndex++;
            playCurrentQueueSong();
            return;
        }

        try {
            stopPlaybackInternal();
            Media media = new Media(path.toUri().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setOnEndOfMedia(() -> {
                advancePlaybackIndex();
                playCurrentQueueSong();
            });
            mediaPlayer.setOnError(() -> statusLabel.setText("Playback error: " + mediaPlayer.getError()));
            mediaPlayer.play();
            statusLabel.setText("Playing playlist: " + currentSong.getSummary());
            updatePermissions(playlistListView.getSelectionModel().getSelectedItem());
        } catch (RuntimeException ex) {
            statusLabel.setText("Could not play song: " + ex.getMessage());
        }
    }

    private Path resolveSongPath(Song song) {
        if (song.getLocalStoragePath() != null && !song.getLocalStoragePath().isBlank()) {
            return Path.of(song.getLocalStoragePath());
        }

        if (song.getStoredFileName() != null && !song.getStoredFileName().isBlank()) {
            try {
                return SongRepository.getSongsDirectory().resolve(song.getStoredFileName());
            } catch (IOException ex) {
                return null;
            }
        }

        return null;
    }

    private void stopPlaybackInternal() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        updatePermissions(playlistListView.getSelectionModel().getSelectedItem());
    }

    private void advancePlaybackIndex() {
        if (repeatMode == RepeatMode.SONG) {
            return;
        }

        playbackIndex++;
        if (playbackIndex >= playbackQueue.size() && repeatMode == RepeatMode.PLAYLIST) {
            playbackIndex = 0;
        }
    }

    private void updateRepeatButtonText() {
        if (repeatModeButton != null) {
            repeatModeButton.setText("Repeat: " + repeatModeToLabel(repeatMode));
        }
    }

    private static String repeatModeToLabel(RepeatMode mode) {
        if (mode == RepeatMode.PLAYLIST) {
            return "Playlist";
        }
        if (mode == RepeatMode.SONG) {
            return "Song";
        }
        return "Off";
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private void goToLibraryWithSelectedSong(Song song) {
        if (song == null) {
            return;
        }

        stopPlaybackInternal();
        App.setPendingSongSelectionId(song.getId());
        try {
            App.setRoot("list");
        } catch (IOException ex) {
            statusLabel.setText("Could not open songs library: " + ex.getMessage());
        }
    }
}
