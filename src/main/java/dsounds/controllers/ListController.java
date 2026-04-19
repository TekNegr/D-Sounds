package dsounds.controllers;

import dsounds.App;
import dsounds.controllers.ReviewController.ReviewStats;
import dsounds.repositories.AlbumRepository;
import dsounds.models.Song;
import dsounds.models.Review;
import dsounds.models.User;
import dsounds.models.UserRole;
import dsounds.security.RoleGuard;
import dsounds.repositories.SongRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.scene.control.Slider;
import javafx.util.Duration;

public class ListController {

    private static final int MAX_COMMENT_LENGTH = 140;

    // Le compteur d'écoutes visiteur est partagé dans App (Laksman)
    // pour éviter qu'un visiteur contourne la limite en changeant d'écran.

    @FXML
    private ListView<Song> songListView;

    @FXML
    private TextArea songDetailsArea;

    @FXML
    private Label songCountLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Button editSongButton;

    @FXML
    private Button pauseButton; // Bouton pause ajouté par Laksman

    @FXML
    private ImageView coverImageView;

    @FXML
    private TextField searchField;

    @FXML
    private Button clearSearchButton;

    @FXML
    private TextArea reviewCommentArea;

    @FXML
    private Label reviewStatsLabel;

    @FXML
    private Button likeButton;

    @FXML
    private Button dislikeButton;

    private final ObservableList<Song> songs = FXCollections.observableArrayList();
    private final ObservableList<Song> allSongs = FXCollections.observableArrayList();
    private MediaPlayer mediaPlayer;
    private boolean isPaused = false; // État pause (Laksman)
    private int currentSongIndex = -1; // Index du morceau en cours (Laksman — prev/next)

    @FXML
    private Button prevButton; // Bouton ⏮ précédent (Laksman)

    @FXML
    private Slider progressSlider; // Barre de progression (Laksman)

    @FXML
    private Label currentTimeLabel; // Temps écoulé (Laksman)

    @FXML
    private Label totalTimeLabel; // Durée totale (Laksman)

    @FXML
    private Label nowPlayingTitle; // Titre en cours (Laksman)

    @FXML
    private Label nowPlayingArtist; // Artiste en cours (Laksman)

    private boolean sliderDragging = false; // Empêche le conflit slider/player

    @FXML
    private Button nextButton; // Bouton ⏭ suivant (Laksman)
    private final ReviewController reviewController = new ReviewController();

    @FXML
    private void initialize() {
        songListView.setItems(songs);
        songListView.setCellFactory(new Callback<ListView<Song>, ListCell<Song>>() {
            @Override
            public ListCell<Song> call(ListView<Song> listView) {
                return new ListCell<Song>() {
                    @Override
                    protected void updateItem(Song song, boolean empty) {
                        super.updateItem(song, empty);
                        if (empty || song == null) {
                            setText(null);
                        } else {
                            setText(song.getSummary());
                        }
                    }
                };
            }
        });

        songListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                songDetailsArea.clear();
                coverImageView.setImage(null);
                reviewCommentArea.clear();
                reviewStatsLabel.setText("No song selected.");
                setReviewControlsDisabled(true);
            } else {
                refreshSelectedSongDetails(newValue);
                updateCoverPreview(newValue);
                refreshReviewPanel(newValue);
            }

            updateEditPermission(newValue);
        });

        searchField.textProperty().addListener((observable, oldValue, newValue) -> applySongFilter());

        refreshSongs();
    }

    @FXML
    private void clearSearch() {
        searchField.clear();
        applySongFilter();
    }

    @FXML
    private void likeSelectedSong() {
        submitReview(true);
    }

    @FXML
    private void dislikeSelectedSong() {
        submitReview(false);
    }

    @FXML
    private void editSelectedSong() {
        Song selectedSong = songListView.getSelectionModel().getSelectedItem();
        if (selectedSong == null) {
            statusLabel.setText("Sélectionnez un morceau.");
            return;
        }

        String previousAlbum = selectedSong.getAlbum();

        if (!canEdit(selectedSong)) {
            statusLabel.setText("Vous ne pouvez modifier que vos propres morceaux, sauf si vous êtes administrateur.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Song");
        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField titleField = new TextField(selectedSong.getTitle());
        TextField artistField = new TextField(selectedSong.getArtist());
        TextField genreField = new TextField(selectedSong.getGenre());
        TextField albumField = new TextField(selectedSong.getAlbum());

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.addRow(0, new Label("Title"), titleField);
        grid.addRow(1, new Label("Artist"), artistField);
        grid.addRow(2, new Label("Genre"), genreField);
        grid.addRow(3, new Label("Album"), albumField);
        pane.setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        selectedSong.setTitle(safeTrim(titleField.getText()));
        selectedSong.setArtist(safeTrim(artistField.getText()));
        selectedSong.setGenre(safeTrim(genreField.getText()));
        selectedSong.setAlbum(safeTrim(albumField.getText()));

        try {
            maybeUpdateCover(selectedSong);
            AlbumRepository.moveSong(previousAlbum, selectedSong.getAlbum(), selectedSong);
            SongRepository.saveSongMetadata(selectedSong);
            songListView.refresh();
            songDetailsArea.setText(selectedSong.getDetails());
            updateCoverPreview(selectedSong);
            applySongFilter();
            statusLabel.setText("Morceau mis à jour.");
        } catch (IOException ex) {
            statusLabel.setText("Could not save changes: " + ex.getMessage());
        }
    }

    @FXML
    private void playSelectedSong() {
        Song selectedSong = songListView.getSelectionModel().getSelectedItem();
        if (selectedSong == null) {
            statusLabel.setText("Sélectionnez un morceau.");
            return;
        }
        int index = songs.indexOf(selectedSong);
        playSongAtIndex(index);
    }

    /**
     * Joue le morceau à l'index donné dans la liste songs.
     * Gère la limite visiteur et mémorise l'index courant (Laksman).
     */
    private void playSongAtIndex(int index) {
        if (index < 0 || index >= songs.size()) return;

        // Limite visiteur — compteur partagé dans App (Laksman).
        if (RoleGuard.isVisitor(App.getAuthController().getSession())) {
            if (App.getVisitorPlayCount() >= App.VISITOR_MAX_PLAYS) {
                statusLabel.setText(
                        "Limite visiteur atteinte : " + App.VISITOR_MAX_PLAYS
                        + " écoutes utilisées. Connectez-vous ou créez un compte.");
                return;
            }
            App.incrementVisitorPlayCount();
        }

        Song song = songs.get(index);
        Path songPath = resolveSongPath(song);
        if (songPath == null || !Files.exists(songPath)) {
            statusLabel.setText("Fichier audio introuvable pour : " + song.getTitle());
            return;
        }

        try {
            stopCurrentPlayback();
            currentSongIndex = index;

            // Sélectionner dans la liste visuelle
            songListView.getSelectionModel().select(index);
            songListView.scrollTo(index);

            Media media = new Media(songPath.toUri().toString());
            mediaPlayer = new MediaPlayer(media);
            App.registerPlayer(mediaPlayer);
            App.setNowPlaying(song);

            mediaPlayer.setOnError(() ->
                statusLabel.setText("Erreur de lecture : " + mediaPlayer.getError()));

            // Barre de progression (Laksman)
            mediaPlayer.currentTimeProperty().addListener((obs, oldT, newT) -> {
                if (!sliderDragging && mediaPlayer != null) {
                    Duration total = mediaPlayer.getTotalDuration();
                    if (total != null && total.greaterThan(Duration.ZERO)) {
                        double progress = newT.toSeconds() / total.toSeconds();
                        if (progressSlider != null) progressSlider.setValue(progress);
                        if (currentTimeLabel != null) currentTimeLabel.setText(formatDuration(newT));
                    }
                }
            });
            mediaPlayer.setOnReady(() -> {
                Duration total = mediaPlayer.getTotalDuration();
                if (totalTimeLabel != null && total != null) totalTimeLabel.setText(formatDuration(total));
                if (progressSlider != null) progressSlider.setValue(0);
            });

            // Auto-passage au morceau suivant en fin de lecture (Laksman)
            mediaPlayer.setOnEndOfMedia(() -> {
                if (currentSongIndex + 1 < songs.size()) {
                    playSongAtIndex(currentSongIndex + 1);
                } else {
                    statusLabel.setText("⏹ Fin de la liste.");
                    stopCurrentPlayback();
                }
            });

            mediaPlayer.play();
            isPaused = false;
            updatePauseButton();
            updateNavButtons();
            statusLabel.setText("▶ " + (index + 1) + "/" + songs.size()
                    + " — " + song.getTitle());
            if (nowPlayingTitle != null) nowPlayingTitle.setText(song.getTitle());
            if (nowPlayingArtist != null) nowPlayingArtist.setText(
                    song.getArtist() != null ? song.getArtist() : "");
        } catch (RuntimeException ex) {
            statusLabel.setText("Impossible de lire le morceau : " + ex.getMessage());
        }
    }

    /**
     * ⏮ Précédent : si > 10 sec écoulées → retour au début.
     * Si ≤ 10 sec → morceau précédent (Laksman).
     */
    @FXML
    private void playPrevious() {
        if (mediaPlayer == null) {
            // Rien en lecture — juste sélectionner le précédent
            int sel = songListView.getSelectionModel().getSelectedIndex();
            if (sel > 0) playSongAtIndex(sel - 1);
            return;
        }
        double elapsed = mediaPlayer.getCurrentTime().toSeconds();
        if (elapsed > 10.0 || currentSongIndex <= 0) {
            // Retour au début du morceau courant
            mediaPlayer.seek(javafx.util.Duration.ZERO);
            isPaused = false;
            mediaPlayer.play();
            updatePauseButton();
            statusLabel.setText("▶ Retour au début — " + songs.get(currentSongIndex).getTitle());
        } else {
            // Morceau précédent
            playSongAtIndex(currentSongIndex - 1);
        }
    }

    /**
     * ⏭ Suivant : passe au morceau suivant dans la liste (Laksman).
     */
    @FXML
    private void playNext() {
        if (currentSongIndex + 1 < songs.size()) {
            playSongAtIndex(currentSongIndex + 1);
        } else {
            statusLabel.setText("Vous êtes déjà sur le dernier morceau.");
        }
    }

    /**
     * Formate une Duration en mm:ss (Laksman).
     */
    private static String formatDuration(Duration d) {
        if (d == null || d.isUnknown() || d.isIndefinite()) return "00:00";
        int totalSec = (int) d.toSeconds();
        return String.format("%02d:%02d", totalSec / 60, totalSec % 60);
    }

    /**
     * Met à jour l'état des boutons prev/next (Laksman).
     */
    private void updateNavButtons() {
        if (prevButton != null) prevButton.setDisable(currentSongIndex <= 0 && (mediaPlayer == null || mediaPlayer.getCurrentTime().toSeconds() <= 10));
        if (nextButton != null) nextButton.setDisable(currentSongIndex >= songs.size() - 1);
    }

    @FXML
    private void stopPlayback() {
        stopCurrentPlayback();
        statusLabel.setText("⏹ Lecture arrêtée.");
    }

    @FXML
    private void refreshSongs() {
        try {
            List<Song> loadedSongs = SongRepository.loadAllLocalSongs();
            allSongs.setAll(loadedSongs);
            applySongFilter();
            statusLabel.setText(loadedSongs.isEmpty() ? "No local songs found yet." : "Library loaded from local storage.");

            if (loadedSongs.isEmpty()) {
                songDetailsArea.setText("No songs have been saved yet.");
                reviewStatsLabel.setText("No song selected.");
            } else if (songListView.getSelectionModel().getSelectedItem() == null) {
                String pendingSongId = App.consumePendingSongSelectionId();
                if (pendingSongId != null) {
                    selectSongById(pendingSongId);
                }

                if (songListView.getSelectionModel().getSelectedItem() == null) {
                    songListView.getSelectionModel().selectFirst();
                }
            }

            Song selectedSong = songListView.getSelectionModel().getSelectedItem();
            if (selectedSong != null) {
                refreshSelectedSongDetails(selectedSong);
                refreshReviewPanel(selectedSong);
            }
        } catch (IOException ex) {
            songCountLabel.setText("Songs found: 0");
            statusLabel.setText("Could not load local songs: " + ex.getMessage());
            songDetailsArea.setText("Failed to read local storage metadata.");
        }
    }

    @FXML
    private void switchToDashboard() throws IOException {
        App.setRoot("browser");
    }

    private void updateEditPermission(Song selectedSong) {
        editSongButton.setDisable(selectedSong == null || !canEdit(selectedSong));
    }

    private void refreshSelectedSongDetails(Song song) {
        StringBuilder builder = new StringBuilder(song.getDetails());
        try {
            ReviewStats stats = reviewController.getStatsForSong(song.getId());
            builder.append("\n\nReviews:\n");
            builder.append("Likes: ").append(stats.getLikes()).append(" | Dislikes: ").append(stats.getDislikes());
        } catch (IOException ex) {
            builder.append("\n\nReviews unavailable.");
        }

        songDetailsArea.setText(builder.toString());
    }

    private void refreshReviewPanel(Song song) {
        User currentUser = App.getAuthController().getSession().getCurrentUser();
        if (currentUser == null) {
            setReviewControlsDisabled(true);
            reviewStatsLabel.setText("Log in to like, dislike, or comment.");
            return;
        }

        setReviewControlsDisabled(false);

        try {
            Review review = reviewController.findCurrentUserReview(song.getId(), currentUser.getId());
            if (review != null) {
                reviewCommentArea.setText(review.getComment() == null ? "" : review.getComment());
                reviewStatsLabel.setText("Your review: " + (review.isLiked() ? "Like" : "Dislike"));
            } else {
                reviewCommentArea.clear();
                reviewStatsLabel.setText("Your review: none");
            }

            ReviewStats stats = reviewController.getStatsForSong(song.getId());
            reviewStatsLabel.setText(reviewStatsLabel.getText() + " | Likes: " + stats.getLikes() + " | Dislikes: " + stats.getDislikes());
        } catch (IOException ex) {
            reviewStatsLabel.setText("Review data unavailable: " + ex.getMessage());
        }
    }

    private void submitReview(boolean liked) {
        Song selectedSong = songListView.getSelectionModel().getSelectedItem();
        if (selectedSong == null) {
            statusLabel.setText("Sélectionnez un morceau.");
            return;
        }

        User currentUser = App.getAuthController().getSession().getCurrentUser();
        if (currentUser == null || currentUser.getRole() == UserRole.VISITOR) {
            statusLabel.setText("Log in to review songs.");
            return;
        }

        String comment = safeTrim(reviewCommentArea.getText());
        if (comment.length() > MAX_COMMENT_LENGTH) {
            reviewCommentArea.setText(comment.substring(0, MAX_COMMENT_LENGTH));
            comment = reviewCommentArea.getText();
        }

        try {
            reviewController.upsertReview(selectedSong.getId(), currentUser.getId(), liked, comment);
            refreshSelectedSongDetails(selectedSong);
            refreshReviewPanel(selectedSong);
            statusLabel.setText("Avis enregistré.");
        } catch (IOException ex) {
            statusLabel.setText("Impossible d'enregistrer l'avis : " + ex.getMessage());
        } catch (AuthException ex) {
            statusLabel.setText("Accès refusé : " + ex.getMessage());
        }
    }

    private void selectSongById(String songId) {
        for (Song song : songs) {
            if (song.getId().equals(songId)) {
                songListView.getSelectionModel().select(song);
                return;
            }
        }
    }

    private void applySongFilter() {
        String query = searchField.getText();
        Song selectedSong = songListView.getSelectionModel().getSelectedItem();
        String selectedId = selectedSong != null ? selectedSong.getId() : null;

        List<Song> filteredSongs = allSongs.stream()
                .filter(song -> SongSearchUtils.matches(song, query))
                .collect(java.util.stream.Collectors.toList());

        songs.setAll(filteredSongs);
        songCountLabel.setText("Songs found: " + filteredSongs.size() + " / " + allSongs.size());

        if (selectedId != null) {
            selectSongById(selectedId);
        }

        if (songListView.getSelectionModel().getSelectedItem() == null && !songs.isEmpty()) {
            songListView.getSelectionModel().selectFirst();
        }

        clearSearchButton.setDisable(query == null || query.isBlank());
    }

    /**
     * Verifie si l'utilisateur peut modifier ce morceau.
     * Utilise RoleGuard pour centraliser la logique (Laksman).
     */
    private boolean canEdit(Song song) {
        return RoleGuard.canModify(
                App.getAuthController().getSession(),
                song.getPublisherUsername());
    }

    private void updateCoverPreview(Song song) {
        Path coverPath = resolveCoverPath(song);
        if (coverPath == null || !Files.exists(coverPath)) {
            coverImageView.setImage(null);
            return;
        }

        try {
            coverImageView.setImage(new Image(coverPath.toUri().toString(), true));
        } catch (IllegalArgumentException ex) {
            coverImageView.setImage(null);
        }
    }

    private Path resolveCoverPath(Song song) {
        if (song.getCoverImagePath() != null && !song.getCoverImagePath().isBlank()) {
            return Path.of(song.getCoverImagePath());
        }

        if (song.getCoverImageFileName() != null && !song.getCoverImageFileName().isBlank()) {
            try {
                return SongRepository.getCoversDirectory().resolve(song.getCoverImageFileName());
            } catch (IOException ex) {
                return null;
            }
        }

        return null;
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

    /**
     * Stoppe et libère proprement le MediaPlayer (Laksman).
     */
    private void stopCurrentPlayback() {
        App.stopGlobalPlayer();
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        isPaused = false;
        updatePauseButton();
        updateNavButtons();
        if (progressSlider != null) progressSlider.setValue(0);
        if (currentTimeLabel != null) currentTimeLabel.setText("00:00");
        if (nowPlayingTitle != null) nowPlayingTitle.setText("Sélectionnez un morceau");
        if (nowPlayingArtist != null) nowPlayingArtist.setText("");
    }

    /**
     * Bascule entre lecture et pause (Laksman).
     */
    @FXML
    private void togglePause() {
        if (mediaPlayer == null) return;
        if (isPaused) {
            mediaPlayer.play();
            isPaused = false;
            statusLabel.setText("▶ Lecture reprise.");
        } else {
            mediaPlayer.pause();
            isPaused = true;
            statusLabel.setText("⏸ En pause.");
        }
        updatePauseButton();
    }

    /**
     * Met à jour le texte du bouton pause (Laksman).
     */
    private void updatePauseButton() {
        if (pauseButton == null) return;
        pauseButton.setDisable(mediaPlayer == null);
        pauseButton.setText(isPaused ? "▶ Reprendre" : "⏸ Pause");
    }

    private void setReviewControlsDisabled(boolean disabled) {
        reviewCommentArea.setDisable(disabled);
        likeButton.setDisable(disabled);
        dislikeButton.setDisable(disabled);
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private void maybeUpdateCover(Song song) throws IOException {
        Alert question = new Alert(Alert.AlertType.CONFIRMATION);
        question.setTitle("Update Cover");
        question.setHeaderText("Do you want to replace the cover image?");
        question.setContentText("Choose No to keep the current cover.");

        ButtonType yes = new ButtonType("Yes");
        ButtonType no = new ButtonType("No");
        question.getButtonTypes().setAll(yes, no, ButtonType.CANCEL);

        Optional<ButtonType> response = question.showAndWait();
        if (response.isEmpty() || response.get() == ButtonType.CANCEL || response.get() == no) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose cover image");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Supported image", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*.*"));

        Stage currentStage = (Stage) songListView.getScene().getWindow();
        java.io.File chosen = chooser.showOpenDialog(currentStage);
        if (chosen == null) {
            return;
        }

        Path chosenPath = chosen.toPath();
        Path coversDir = SongRepository.getCoversDirectory();
        String coverFileName = song.getId() + "-" + sanitizeFileName(chosenPath.getFileName().toString());
        Path targetCoverPath = coversDir.resolve(coverFileName);
        Files.copy(chosenPath, targetCoverPath, StandardCopyOption.REPLACE_EXISTING);
        song.setCoverImageFileName(coverFileName);
        song.setCoverImagePath(targetCoverPath.toString());
    }

    private static String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
