package dsounds.controllers;

import dsounds.App;
import dsounds.controllers.ReviewController.ReviewStats;
import dsounds.repositories.AlbumRepository;
import dsounds.models.Song;
import dsounds.models.Review;
import dsounds.models.User;
import dsounds.models.UserRole;
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

public class ListController {

    private static final int MAX_COMMENT_LENGTH = 140;

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
            statusLabel.setText("Select a song first.");
            return;
        }

        String previousAlbum = selectedSong.getAlbum();

        if (!canEdit(selectedSong)) {
            statusLabel.setText("You can only edit your own songs unless you are admin.");
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
            statusLabel.setText("Song updated.");
        } catch (IOException ex) {
            statusLabel.setText("Could not save changes: " + ex.getMessage());
        }
    }

    @FXML
    private void playSelectedSong() {
        Song selectedSong = songListView.getSelectionModel().getSelectedItem();
        if (selectedSong == null) {
            statusLabel.setText("Select a song first.");
            return;
        }

        Path songPath = resolveSongPath(selectedSong);
        if (songPath == null || !Files.exists(songPath)) {
            statusLabel.setText("Audio file not found for selected song.");
            return;
        }

        try {
            stopCurrentPlayback();
            Media media = new Media(songPath.toUri().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setOnError(() -> statusLabel.setText("Playback error: " + mediaPlayer.getError()));
            mediaPlayer.setOnEndOfMedia(() -> statusLabel.setText("Playback finished: " + selectedSong.getTitle()));
            mediaPlayer.play();
            statusLabel.setText("Playing: " + selectedSong.getSummary());
        } catch (RuntimeException ex) {
            statusLabel.setText("Could not play song: " + ex.getMessage());
        }
    }

    @FXML
    private void stopPlayback() {
        stopCurrentPlayback();
        statusLabel.setText("Playback stopped.");
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
        stopCurrentPlayback();
        App.setRoot("dashboard");
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
            statusLabel.setText("Select a song first.");
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
            statusLabel.setText("Review saved.");
        } catch (IOException ex) {
            statusLabel.setText("Could not save review: " + ex.getMessage());
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

    private boolean canEdit(Song song) {
        User currentUser = App.getAuthController().getSession().getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        if (currentUser.getRole() == UserRole.ADMIN) {
            return true;
        }

        String publisher = song.getPublisherUsername();
        return publisher != null && publisher.equalsIgnoreCase(currentUser.getUsername());
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

    private void stopCurrentPlayback() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
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
