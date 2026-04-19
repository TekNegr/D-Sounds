package dsounds.controllers;

import dsounds.App;
import dsounds.repositories.AlbumRepository;
import dsounds.security.RoleGuard;
import dsounds.models.Song;
import dsounds.models.User;
import dsounds.repositories.SongRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ArtistController {

    private static final Set<String> SUPPORTED_AUDIO_EXTENSIONS = Set.of("mp3", "wav", "m4a");
    private static final Set<String> SUPPORTED_IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp");

    @FXML
    private TextField titleField;

    @FXML
    private TextField artistField;

    @FXML
    private TextField genreField;

    @FXML
    private ComboBox<String> albumComboBox;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private TextArea lyricsArea;

    @FXML
    private Label selectedFileLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Label selectedCoverLabel;

    @FXML
    private Label publisherLabel;

    private Path selectedSongFile;
    private Path selectedCoverFile;

    @FXML
    private void initialize() {
        // Vérification de rôle : seuls les admins peuvent accéder à cette page (Laksman).
        if (!RoleGuard.canManageCatalog(App.getAuthController().getSession())) {
            try {
                statusLabel.setText("Accès refusé : réservé aux administrateurs.");
                App.setRoot("dashboard");
            } catch (java.io.IOException ignored) {}
            return;
        }
        albumComboBox.setEditable(true);
        User currentUser = App.getAuthController().getSession().getCurrentUser();
        String publisher = currentUser == null ? "visiteur" : currentUser.getUsername();
        publisherLabel.setText(publisher);
        refreshAlbumChoices();
    }

    @FXML
    private void chooseSongFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir un fichier audio");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Audio supporté", "*.mp3", "*.wav", "*.m4a")
        );
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );

        Stage currentStage = (Stage) titleField.getScene().getWindow();
        java.io.File chosen = chooser.showOpenDialog(currentStage);
        if (chosen == null) {
            return;
        }

        selectedSongFile = chosen.toPath();
        selectedFileLabel.setText(selectedSongFile.getFileName().toString());
        statusLabel.setText("Song selected. You can now save it.");
    }

    @FXML
    private void saveSongLocally() {
        String title = safeTrim(titleField.getText());
        String artist = safeTrim(artistField.getText());

        if (selectedSongFile == null) {
            statusLabel.setText("Select an audio file first.");
            return;
        }

        if (!hasSupportedAudioExtension(selectedSongFile)) {
            statusLabel.setText("Unsupported file type. Use MP3, WAV, or M4A.");
            return;
        }

        if (title.isEmpty() || artist.isEmpty()) {
            statusLabel.setText("Le titre et l'artiste sont obligatoires.");
            return;
        }

        try {
            // Create and populate Song model
            Song song = new Song();
            song.setTitle(title);
            song.setArtist(artist);
            song.setGenre(safeTrim(genreField.getText()));
            song.setAlbum(safeTrim(albumComboBox.getEditor().getText()));
            song.setDescription(safeTrim(descriptionArea.getText()));
            song.setLyrics(safeTrim(lyricsArea.getText()));
            song.setOriginalFileName(selectedSongFile.getFileName().toString());
            song.setStorageLocation(Song.StorageLocation.LOCAL);
            song.setPublisherUsername(publisherLabel.getText());

            // Determine MIME type
            String fileName = selectedSongFile.getFileName().toString().toLowerCase();
            if (fileName.endsWith(".wav")) {
                song.setMimeType("audio/wav");
            } else if (fileName.endsWith(".m4a")) {
                song.setMimeType("audio/mp4");
            } else {
                song.setMimeType("audio/mpeg");
            }

            // Copy file to local storage
            Path songsDir = SongRepository.getSongsDirectory();
            String storedFileName = song.getId() + "-" + sanitizeFileName(selectedSongFile.getFileName().toString());
            Path targetSongPath = songsDir.resolve(storedFileName);

            Files.copy(selectedSongFile, targetSongPath, StandardCopyOption.REPLACE_EXISTING);

            // Update song with storage paths
            song.setStoredFileName(storedFileName);
            song.setLocalStoragePath(targetSongPath.toString());

            if (selectedCoverFile != null) {
                Path coversDir = SongRepository.getCoversDirectory();
                String coverFileName = song.getId() + "-" + sanitizeFileName(selectedCoverFile.getFileName().toString());
                Path targetCoverPath = coversDir.resolve(coverFileName);
                Files.copy(selectedCoverFile, targetCoverPath, StandardCopyOption.REPLACE_EXISTING);
                song.setCoverImageFileName(coverFileName);
                song.setCoverImagePath(targetCoverPath.toString());
            }

            if (!safeTrim(song.getAlbum()).isEmpty()) {
                AlbumRepository.registerSong(song.getAlbum(), song);
            }

            // Save metadata
            SongRepository.saveSongMetadata(song);

            statusLabel.setText("Saved locally: " + storedFileName);
            clearForm();
            refreshAlbumChoices();
        } catch (IOException ex) {
            statusLabel.setText("Impossible d'enregistrer le morceau : " + ex.getMessage());
        }
    }

    @FXML
    private void switchToDashboard() throws IOException {
        App.setRoot("dashboard");
    }

    @FXML
    private void chooseCoverImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une pochette");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Supported image", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );

        Stage currentStage = (Stage) titleField.getScene().getWindow();
        java.io.File chosen = chooser.showOpenDialog(currentStage);
        if (chosen == null) {
            return;
        }

        Path coverPath = chosen.toPath();
        if (!hasSupportedImageExtension(coverPath)) {
            statusLabel.setText("Unsupported cover type. Use PNG, JPG, JPEG or WEBP.");
            return;
        }

        selectedCoverFile = coverPath;
        selectedCoverLabel.setText(selectedCoverFile.getFileName().toString());
        statusLabel.setText("Cover selected.");
    }

    private void clearForm() {
        titleField.clear();
        artistField.clear();
        genreField.clear();
        albumComboBox.getEditor().clear();
        albumComboBox.getSelectionModel().clearSelection();
        descriptionArea.clear();
        lyricsArea.clear();
        selectedSongFile = null;
        selectedCoverFile = null;
        selectedFileLabel.setText("No file selected");
        selectedCoverLabel.setText("No cover selected");
    }

    private void refreshAlbumChoices() {
        try {
            String currentText = albumComboBox.getEditor().getText();
            albumComboBox.getItems().setAll(AlbumRepository.loadAlbumNames());
            albumComboBox.getEditor().setText(currentText);
        } catch (IOException ex) {
            statusLabel.setText("Could not load albums: " + ex.getMessage());
        }
    }

    private static String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean hasSupportedAudioExtension(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return false;
        }

        String extension = fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        return SUPPORTED_AUDIO_EXTENSIONS.contains(extension);
    }

    private static boolean hasSupportedImageExtension(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return false;
        }

        String extension = fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        return SUPPORTED_IMAGE_EXTENSIONS.contains(extension);
    }
}
