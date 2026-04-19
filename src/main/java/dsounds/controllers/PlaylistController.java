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
import dsounds.security.RoleGuard;
import dsounds.security.OwnershipChecker;

/**
 * PlaylistController handles playlist creation, management, and song operations.
 */
public class PlaylistController {

    /**
     * Gestionnaire d'ownership et d'accès collaboratif aux playlists.
     * Instancié une fois par contrôleur — partage le même registre pour toute la session.
     *
     * <b>Ajouté par Laksman</b> — ownership checks et accès collaboratif.
     */
    private final OwnershipChecker ownershipChecker = new OwnershipChecker();

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

    // ---- Collaborator UI fields (Laksman) ----
    @FXML
    private javafx.scene.control.TextField collabUsernameField;

    @FXML
    private javafx.scene.control.ComboBox<String> collabRoleCombo;

    @FXML
    private javafx.scene.control.ListView<String> collabListView;

    @FXML
    private Button addCollabButton;

    @FXML
    private Button removeCollabButton;

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

        // Initialize collaborator role combo (Laksman).
        if (collabRoleCombo != null) {
            collabRoleCombo.getItems().addAll("EDITOR", "VIEWER");
            collabRoleCombo.setValue("EDITOR");
        }
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
            // Charger les collaborateurs persistés pour chaque playlist (Laksman).
            for (Playlist pl : loadedPlaylists) {
                ownershipChecker.loadFromPlaylist(pl);
            }
            playlists.setAll(filterVisiblePlaylists(loadedPlaylists));
            statusLabel.setText("Playlists chargées : " + playlists.size());

            if (!playlists.isEmpty() && playlistListView.getSelectionModel().getSelectedItem() == null) {
                playlistListView.getSelectionModel().selectFirst();
            }

            applySongSearch();
            updatePermissions(playlistListView.getSelectionModel().getSelectedItem());
        } catch (IOException ex) {
            statusLabel.setText("Échec du rafraîchissement des playlists : " + ex.getMessage());
        }
    }

    @FXML
    private void createPlaylist() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            statusLabel.setText("Vous devez être connecté.");
            return;
        }
        // Vérification centralisée via RoleGuard (Laksman — défense en profondeur).
        if (!RoleGuard.canCreatePlaylist(App.getAuthController().getSession())) {
            statusLabel.setText("Les visiteurs ne peuvent pas créer de playlists. Veuillez vous connecter.");
            return;
        }

        String playlistName = safeTrim(playlistNameField.getText());
        if (playlistName.isEmpty()) {
            statusLabel.setText("Le nom de la playlist est obligatoire.");
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
            statusLabel.setText("Playlist créée.");
        } catch (IOException ex) {
            statusLabel.setText("Impossible de créer la playlist : " + ex.getMessage());
        }
    }

    @FXML
    private void updatePlaylist() {
        Playlist selectedPlaylist = playlistListView.getSelectionModel().getSelectedItem();
        if (selectedPlaylist == null) {
            statusLabel.setText("Sélectionnez d'abord une playlist.");
            return;
        }

        if (!canEdit(selectedPlaylist)) {
            statusLabel.setText("Seuls les administrateurs ou le propriétaire peuvent modifier cette playlist.");
            return;
        }

        String playlistName = safeTrim(playlistNameField.getText());
        if (playlistName.isEmpty()) {
            statusLabel.setText("Le nom de la playlist est obligatoire.");
            return;
        }

        selectedPlaylist.setName(playlistName);
        selectedPlaylist.setDescription(safeTrim(playlistDescriptionArea.getText()));
        selectedPlaylist.setVisibility(publicPlaylistCheckBox.isSelected() ? Playlist.Visibility.PUBLIC : Playlist.Visibility.PRIVATE);

        try {
            PlaylistRepository.savePlaylist(selectedPlaylist);
            refreshAll();
            selectPlaylistById(selectedPlaylist.getId());
            statusLabel.setText("Playlist mise à jour.");
        } catch (IOException ex) {
            statusLabel.setText("Impossible de mettre à jour la playlist : " + ex.getMessage());
        }
    }

    @FXML
    private void deletePlaylist() {
        Playlist selectedPlaylist = playlistListView.getSelectionModel().getSelectedItem();
        if (selectedPlaylist == null) {
            statusLabel.setText("Sélectionnez d'abord une playlist.");
            return;
        }

        if (!canEdit(selectedPlaylist)) {
            statusLabel.setText("Seuls les administrateurs ou le propriétaire peuvent supprimer cette playlist.");
            return;
        }

        try {
            PlaylistRepository.deletePlaylist(selectedPlaylist.getId());
            ownershipChecker.clearCollaborators(selectedPlaylist.getId()); // Laksman: cleanup collaborators on delete.
            refreshAll();
            clearPlaylistForm();
            statusLabel.setText("Playlist supprimée.");
        } catch (IOException ex) {
            statusLabel.setText("Impossible de supprimer la playlist : " + ex.getMessage());
        }
    }

    @FXML
    private void addSelectedSongToPlaylist() {
        Playlist selectedPlaylist = playlistListView.getSelectionModel().getSelectedItem();
        Song selectedSong = availableSongsListView.getSelectionModel().getSelectedItem();

        if (selectedPlaylist == null || selectedSong == null) {
            statusLabel.setText("Sélectionnez une playlist et un morceau.");
            return;
        }

        if (!canEdit(selectedPlaylist)) {
            statusLabel.setText("Seuls les administrateurs ou le propriétaire peuvent modifier cette playlist.");
            return;
        }

        selectedPlaylist.addSong(selectedSong.getId());
        try {
            PlaylistRepository.savePlaylist(selectedPlaylist);
            loadPlaylistSongs(selectedPlaylist);
            statusLabel.setText("Morceau ajouté à la playlist.");
        } catch (IOException ex) {
            statusLabel.setText("Impossible d'ajouter le morceau : " + ex.getMessage());
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
            statusLabel.setText("Seuls les administrateurs ou le propriétaire peuvent modifier cette playlist.");
            return;
        }

        selectedPlaylist.removeSong(selectedSong.getId());
        try {
            PlaylistRepository.savePlaylist(selectedPlaylist);
            loadPlaylistSongs(selectedPlaylist);
            statusLabel.setText("Morceau retiré de la playlist.");
        } catch (IOException ex) {
            statusLabel.setText("Impossible de retirer le morceau : " + ex.getMessage());
        }
    }

    /**
     * Ajoute un collaborateur a la playlist selectionnee (Laksman).
     * Utilise OwnershipChecker pour verifier les droits.
     */
    @FXML
    private void addCollaborator() {
        Playlist selected = playlistListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Sélectionnez d'abord une playlist.");
            return;
        }
        if (collabUsernameField == null || collabUsernameField.getText().isBlank()) {
            statusLabel.setText("Saisissez un nom d'utilisateur à ajouter comme collaborateur.");
            return;
        }
        String username = collabUsernameField.getText().trim();
        String roleStr = collabRoleCombo != null ? collabRoleCombo.getValue() : "EDITOR";
        dsounds.security.OwnershipChecker.CollabRole role =
                "VIEWER".equals(roleStr)
                ? dsounds.security.OwnershipChecker.CollabRole.VIEWER
                : dsounds.security.OwnershipChecker.CollabRole.EDITOR;
        try {
            ownershipChecker.addCollaborator(
                    App.getAuthController().getSession(), selected, username, role);
            // Persister les collaborateurs dans le fichier playlist (Laksman).
            ownershipChecker.syncToPlaylist(selected);
            PlaylistRepository.savePlaylist(selected);
            statusLabel.setText("Collaborateur ajouté : " + username + " [" + roleStr + "]");
            collabUsernameField.clear();
            refreshCollabList(selected);
        } catch (dsounds.controllers.AuthException | IOException e) {
            statusLabel.setText(e.getMessage());
        }
    }

    /**
     * Retire le collaborateur selectionne de la playlist (Laksman).
     */
    @FXML
    private void removeCollaborator() {
        Playlist selected = playlistListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Sélectionnez d'abord une playlist.");
            return;
        }
        String selectedCollab = collabListView != null
                ? collabListView.getSelectionModel().getSelectedItem() : null;
        if (selectedCollab == null || selectedCollab.isBlank()) {
            statusLabel.setText("Sélectionnez un collaborateur à retirer.");
            return;
        }
        // Extract username (format: "username [ROLE]")
        String username = selectedCollab.contains(" [")
                ? selectedCollab.substring(0, selectedCollab.indexOf(" [")).trim()
                : selectedCollab.trim();
        try {
            ownershipChecker.removeCollaborator(
                    App.getAuthController().getSession(), selected, username);
            // Persister les collaborateurs dans le fichier playlist (Laksman).
            ownershipChecker.syncToPlaylist(selected);
            PlaylistRepository.savePlaylist(selected);
            statusLabel.setText("Collaborateur retiré : " + username);
            refreshCollabList(selected);
        } catch (dsounds.controllers.AuthException | IOException e) {
            statusLabel.setText(e.getMessage());
        }
    }

    /** Rafraichit la liste des collaborateurs affichee (Laksman). */
    private void refreshCollabList(Playlist playlist) {
        if (collabListView == null || playlist == null) return;
        java.util.List<String> entries = new java.util.ArrayList<>();
        for (String name : ownershipChecker.getCollaborators(playlist.getId())) {
            dsounds.security.OwnershipChecker.CollabRole r =
                    ownershipChecker.getCollabRole(playlist.getId(), name);
            entries.add(name + " [" + (r != null ? r : "?") + "]");
        }
        collabListView.setItems(javafx.collections.FXCollections.observableArrayList(entries));
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
            statusLabel.setText("Sélectionnez d'abord une playlist.");
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
            statusLabel.setText("Sélectionnez d'abord une playlist.");
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
        refreshCollabList(playlist); // Laksman: refresh collaborators on playlist selection.
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

    /**
     * Vérifie si l'utilisateur courant peut modifier la playlist sélectionnée.
     *
     * <p>Délègue à {@link OwnershipChecker#canEdit} qui prend en compte :
     * admins (toujours), propriétaire, et collaborateurs EDITOR.</p>
     *
     * <b>Modifié par Laksman</b> — utilise OwnershipChecker pour l'ownership
     * et la gestion collaborative.
     */
    private boolean canEdit(Playlist playlist) {
        return ownershipChecker.canEdit(
                App.getAuthController().getSession(), playlist);
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
        // Utilise RoleGuard pour centraliser la logique (Laksman).
        boolean canCreate = RoleGuard.canCreatePlaylist(App.getAuthController().getSession());
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
            statusLabel.setText("Lecture de la playlist terminée.");
            return;
        }

        Song currentSong = playbackQueue.get(playbackIndex);
        Path path = resolveSongPath(currentSong);
        if (path == null || !Files.exists(path)) {
            statusLabel.setText("Fichier audio manquant pour : " + currentSong.getTitle());
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
            mediaPlayer.setOnError(() -> statusLabel.setText("Erreur de lecture : " + mediaPlayer.getError()));
            mediaPlayer.play();
            statusLabel.setText("Lecture de la playlist : " + currentSong.getSummary());
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
