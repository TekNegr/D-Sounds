package dsounds.controllers;

import dsounds.App;
import dsounds.models.Album;
import dsounds.models.Playlist;
import dsounds.models.Review;
import dsounds.models.Song;
import dsounds.models.User;
import dsounds.models.UserRole;
import dsounds.security.RoleGuard;
import dsounds.repositories.AlbumRepository;
import dsounds.repositories.PlaylistRepository;
import dsounds.repositories.ReviewRepository;
import dsounds.repositories.SongRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Callback;

/**
 * BrowserController - Main browser for searching and browsing songs, albums, artists, and playlists
 */
public class BrowserController {

    private enum BrowseType {
        SONGS("Morceaux"),
        ALBUMS("Albums"),
        ARTISTS("Artistes"),
        PLAYLISTS("Playlists");

        private final String label;
        BrowseType(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static class BrowseItem {
        String title;
        String subtitle;
        BrowseType type;
        Object data; // Song, Album, Playlist, or Publisher name

        BrowseItem(String title, String subtitle, BrowseType type, Object data) {
            this.title = title;
            this.subtitle = subtitle;
            this.type = type;
            this.data = data;
        }
    }

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<BrowseType> typeComboBox;

    @FXML
    private ComboBox<String> genreComboBox;

    @FXML
    private Button clearFiltersButton;

    @FXML
    private ListView<BrowseItem> resultsListView;

    @FXML
    private VBox detailPanel;

    @FXML
    private Label detailTitleLabel;

    @FXML
    private Label detailTypeLabel;

    @FXML
    private ImageView detailCoverImageView;

    @FXML
    private TextArea detailInfoArea;

    @FXML
    private VBox detailSongsContainer;

    @FXML
    private ListView<Song> detailSongsListView;

    @FXML
    private Button playButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Label playStatusLabel;

    @FXML
    private VBox reviewsContainer;

    @FXML
    private TextArea reviewCommentArea;

    @FXML
    private Label reviewStatsLabel;

    @FXML
    private Button likeButton;

    @FXML
    private Button dislikeButton;

    private User currentUser;
    private MediaPlayer mediaPlayer;
    private boolean isPaused = false; // État pause (Laksman)
    private List<Song> currentPlayQueue = new ArrayList<>();
    private int currentPlayIndex = -1;

    // Le compteur d'écoutes visiteur est partagé dans App (Laksman)
    // pour éviter qu'un visiteur contourne la limite en changeant d'écran.

    @FXML
    public void initialize() {
        currentUser = getCurrentUser();

        setupTypeComboBox();
        setupGenreComboBox();
        setupResultsListView();
        setupDetailPanel();
        setupSearchListeners();

        performSearch();
        selectPendingSongIfAny();
    }

    private User getCurrentUser() {
        return App.getAuthController().getSession().getCurrentUser();
    }

    private void setupTypeComboBox() {
        typeComboBox.setItems(FXCollections.observableArrayList(BrowseType.values()));
        typeComboBox.setValue(BrowseType.SONGS);
        typeComboBox.valueProperty().addListener((obs, old, newVal) -> performSearch());
    }

    private void setupGenreComboBox() {
        genreComboBox.setOnShowing(e -> refreshGenreList());
        genreComboBox.valueProperty().addListener((obs, old, newVal) -> performSearch());
    }

    private void refreshGenreList() {
        try {
            Set<String> genres = new HashSet<>();
            genres.add("All Genres");

            List<Song> allSongs = SongRepository.loadAllLocalSongs();
            allSongs.forEach(song -> {
                if (song.getGenre() != null && !song.getGenre().isBlank()) {
                    genres.add(song.getGenre());
                }
            });

            List<String> sortedGenres = genres.stream().sorted().collect(Collectors.toList());
            genreComboBox.setItems(FXCollections.observableArrayList(sortedGenres));
            if (genreComboBox.getValue() == null) {
                genreComboBox.setValue("All Genres");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupResultsListView() {
        resultsListView.setCellFactory(new Callback<ListView<BrowseItem>, ListCell<BrowseItem>>() {
            @Override
            public ListCell<BrowseItem> call(ListView<BrowseItem> param) {
                return new ListCell<BrowseItem>() {
                    @Override
                    protected void updateItem(BrowseItem item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            VBox cellVBox = new VBox(4);
                            cellVBox.setStyle("-fx-padding: 8;");

                            Label titleLabel = new Label(item.title);
                            titleLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

                            Label subtitleLabel = new Label(item.subtitle);
                            subtitleLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #666;");

                            Label typeLabel = new Label("[" + item.type.label + "]");
                            typeLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #999;");

                            cellVBox.getChildren().addAll(titleLabel, subtitleLabel, typeLabel);
                            setGraphic(cellVBox);
                        }
                    }
                };
            }
        });

        resultsListView.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                showDetail(newVal);
            } else {
                clearDetailPanel();
            }
        });
    }

    private void setupDetailPanel() {
        detailCoverImageView.setFitWidth(200);
        detailCoverImageView.setFitHeight(200);
        detailCoverImageView.setPreserveRatio(true);

        detailSongsListView.setCellFactory(new Callback<ListView<Song>, ListCell<Song>>() {
            @Override
            public ListCell<Song> call(ListView<Song> param) {
                return new ListCell<Song>() {
                    @Override
                    protected void updateItem(Song song, boolean empty) {
                        super.updateItem(song, empty);
                        if (empty || song == null) {
                            setText(null);
                        } else {
                            setText(song.getTitle() + " - " + song.getArtist());
                        }
                    }
                };
            }
        });
    }

    private void setupSearchListeners() {
        searchField.textProperty().addListener((obs, old, newVal) -> performSearch());
        clearFiltersButton.setOnAction(e -> {
            searchField.clear();
            genreComboBox.setValue("All Genres");
            typeComboBox.setValue(BrowseType.SONGS);
        });
    }

    private void selectPendingSongIfAny() {
        String pendingSongId = App.consumePendingSongSelectionId();
        if (pendingSongId == null || pendingSongId.isBlank()) {
            return;
        }
        for (BrowseItem item : resultsListView.getItems()) {
            if (item.type == BrowseType.SONGS && item.data instanceof Song) {
                Song s = (Song) item.data;
                if (pendingSongId.equals(s.getId())) {
                    resultsListView.getSelectionModel().select(item);
                    resultsListView.scrollTo(item);
                    break;
                }
            }
        }
    }

    private void performSearch() {
        try {
            String query = searchField.getText().trim().toLowerCase();
            String selectedGenre = genreComboBox.getValue();
            if (selectedGenre == null || selectedGenre.equals("All Genres")) {
                selectedGenre = null;
            }
            BrowseType type = typeComboBox.getValue();

            List<BrowseItem> results = new ArrayList<>();

            switch (type) {
                case SONGS:
                    results.addAll(searchSongs(query, selectedGenre));
                    break;
                case ALBUMS:
                    results.addAll(searchAlbums(query, selectedGenre));
                    break;
                case ARTISTS:
                    results.addAll(searchArtists(query));
                    break;
                case PLAYLISTS:
                    results.addAll(searchPlaylists(query));
                    break;
            }

            ObservableList<BrowseItem> itemList = FXCollections.observableArrayList(results);
            resultsListView.setItems(itemList);

            if (results.isEmpty()) {
                clearDetailPanel();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<BrowseItem> searchSongs(String query, String genreFilter) throws IOException {
        List<Song> allSongs = SongRepository.loadAllLocalSongs();
        return allSongs.stream()
            .filter(song -> {
                boolean matchesQuery = query.isEmpty() ||
                    song.getTitle().toLowerCase().contains(query) ||
                    song.getArtist().toLowerCase().contains(query) ||
                    song.getAlbum().toLowerCase().contains(query) ||
                    song.getPublisherUsername().toLowerCase().contains(query);

                boolean matchesGenre = genreFilter == null || song.getGenre().equals(genreFilter);
                return matchesQuery && matchesGenre;
            })
            .map(song -> new BrowseItem(
                song.getTitle(),
                song.getArtist() + " (" + song.getAlbum() + ")",
                BrowseType.SONGS,
                song
            ))
            .collect(Collectors.toList());
    }

    private List<BrowseItem> searchAlbums(String query, String genreFilter) throws IOException {
        List<Album> allAlbums = AlbumRepository.loadAllAlbums();
        List<Song> allSongs = SongRepository.loadAllLocalSongs();

        // Build map of songs by album
        Map<String, List<Song>> songsByAlbum = allSongs.stream()
            .collect(Collectors.groupingBy(Song::getAlbum));

        return allAlbums.stream()
            .filter(album -> query.isEmpty() || album.getName().toLowerCase().contains(query))
            .map(album -> {
                List<Song> albumSongs = songsByAlbum.getOrDefault(album.getName(), new ArrayList<>());

                return new BrowseItem(
                    album.getName(),
                    albumSongs.size() + " songs",
                    BrowseType.ALBUMS,
                    album
                );
            })
            .filter(item -> genreFilter == null || songsByAlbum.getOrDefault(item.title, new ArrayList<>()).stream()
                .anyMatch(s -> s.getGenre().equals(genreFilter)))
            .collect(Collectors.toList());
    }

    private List<BrowseItem> searchArtists(String query) throws IOException {
        List<Song> allSongs = SongRepository.loadAllLocalSongs();

        Set<String> artists = allSongs.stream()
            .filter(song -> query.isEmpty() || song.getArtist().toLowerCase().contains(query))
            .map(Song::getArtist)
            .collect(Collectors.toSet());

        List<Song> allSongsList = allSongs;

        return artists.stream()
            .map(artist -> {
                List<Song> artistSongs = allSongsList.stream()
                    .filter(s -> s.getArtist().equals(artist))
                    .collect(Collectors.toList());

                return new BrowseItem(
                    artist,
                    artistSongs.size() + " songs",
                    BrowseType.ARTISTS,
                    artist
                );
            })
            .sorted((a, b) -> a.title.compareTo(b.title))
            .collect(Collectors.toList());
    }

    /**
     * ⏮ Précédent : si > 10 sec → retour au début. Sinon → morceau précédent (Laksman).
     */
    @FXML
    private void playPrevious() {
        if (mediaPlayer == null) return;
        double elapsed = mediaPlayer.getCurrentTime().toSeconds();
        if (elapsed > 10.0 || currentPlayIndex <= 0) {
            // Retour au début du morceau courant
            mediaPlayer.seek(javafx.util.Duration.ZERO);
            isPaused = false;
            mediaPlayer.play();
            if (!currentPlayQueue.isEmpty() && currentPlayIndex >= 0) {
                playStatusLabel.setText("▶ Retour au début — "
                        + currentPlayQueue.get(currentPlayIndex).getTitle());
            }
        } else {
            // Morceau précédent dans la queue
            currentPlayIndex--;
            playSong(currentPlayQueue.get(currentPlayIndex));
        }
    }

    /**
     * ⏭ Suivant : passe au morceau suivant dans la queue (Laksman).
     */
    @FXML
    private void playNext() {
        if (currentPlayQueue.isEmpty()) return;
        if (currentPlayIndex + 1 < currentPlayQueue.size()) {
            currentPlayIndex++;
            playSong(currentPlayQueue.get(currentPlayIndex));
        } else {
            playStatusLabel.setText("Vous êtes déjà sur le dernier morceau.");
        }
    }

    /**
     * Stoppe et libère proprement le MediaPlayer.
     * Appelé avant chaque nouvelle lecture et avant de quitter l'écran (Laksman).
     */
    private void stopAndDisposePlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        isPaused = false;
    }

    /**
     * Bascule entre lecture et pause (Laksman).
     * Appelé par le bouton ⏸/▶ affiché pendant la lecture.
     */
    @FXML
    private void togglePause() {
        if (mediaPlayer == null) return;
        if (isPaused) {
            mediaPlayer.play();
            isPaused = false;
            playStatusLabel.setText("▶ Lecture reprise.");
        } else {
            mediaPlayer.pause();
            isPaused = true;
            playStatusLabel.setText("⏸ En pause.");
        }
    }

    private List<BrowseItem> searchPlaylists(String query) throws IOException {
        List<Playlist> allPlaylists = PlaylistRepository.loadAllPlaylists();
        return allPlaylists.stream()
            .filter(pl -> {
                // Filtre les playlists privées selon le rôle et l'ownership (Laksman).
                if (pl.getVisibility() == Playlist.Visibility.PRIVATE) {
                    // Visiteur non connecté ou null : ne voit aucune playlist privée.
                    if (currentUser == null || RoleGuard.isVisitor(App.getAuthController().getSession())) {
                        return false;
                    }
                    // Admin : voit toutes les playlists.
                    if (currentUser.getRole() == UserRole.ADMIN) {
                        return true;
                    }
                    // Abonné : voit seulement ses propres playlists privées.
                    if (pl.getOwnerUsername() == null ||
                            !pl.getOwnerUsername().equals(currentUser.getUsername())) {
                        return false;
                    }
                }
                return query.isEmpty() || pl.getName().toLowerCase().contains(query);
            })
            .map(pl -> {
                String visibility = pl.getVisibility() == Playlist.Visibility.PUBLIC ? "Publique" : "Privée";
                String subtitle = pl.getOwnerUsername() + " • " + pl.getSongIds().size() + " songs • " + visibility;

                return new BrowseItem(
                    pl.getName(),
                    subtitle,
                    BrowseType.PLAYLISTS,
                    pl
                );
            })
            .sorted((a, b) -> a.title.compareTo(b.title))
            .collect(Collectors.toList());
    }

    private void showDetail(BrowseItem item) {
        detailPanel.getChildren().clear();
        detailTitleLabel.setText(item.title);
        detailTypeLabel.setText(item.type.label);
        currentPlayQueue.clear();
        currentPlayIndex = -1;

        try {
            switch (item.type) {
                case SONGS:
                    showSongDetail((Song) item.data);
                    break;
                case ALBUMS:
                    showAlbumDetail((Album) item.data);
                    break;
                case ARTISTS:
                    showArtistDetail((String) item.data);
                    break;
                case PLAYLISTS:
                    showPlaylistDetail((Playlist) item.data);
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showSongDetail(Song song) throws IOException {
        VBox detailBox = new VBox(10);
        detailBox.setStyle("-fx-padding: 15;");

        // Cover image
        if (song.getCoverImagePath() != null && !song.getCoverImagePath().isBlank()) {
            try {
                Image img = new Image(new java.io.FileInputStream(song.getCoverImagePath()));
                detailCoverImageView.setImage(img);
                detailBox.getChildren().add(detailCoverImageView);
            } catch (IOException ignored) {}
        }

        // Info
        String info = String.format(
            "Artist: %s\nAlbum: %s\nGenre: %s\nPublisher: %s",
            song.getArtist(), song.getAlbum(), song.getGenre(), song.getPublisherUsername()
        );
        detailInfoArea.setText(info);
        detailBox.getChildren().add(new Label("Info:"));
        detailBox.getChildren().add(detailInfoArea);

        // Reviews
        addReviewPanel(detailBox, song);

        // Play button
        currentPlayQueue.add(song);
        playButton.setOnAction(e -> playSong(song));
        HBox actionBox = new HBox(10);
        actionBox.setPrefHeight(40);
        actionBox.setAlignment(Pos.CENTER);
        actionBox.getChildren().add(playButton);

        boolean canManageSong = canManageSong(song);
        editButton.setDisable(!canManageSong);
        deleteButton.setDisable(!canManageSong);
        if (canManageSong) {
            editButton.setOnAction(e -> editSong(song));
            deleteButton.setOnAction(e -> deleteSong(song));
            actionBox.getChildren().addAll(editButton, deleteButton);
        }

        VBox.setVgrow(detailInfoArea, Priority.SOMETIMES);
        detailBox.getChildren().add(actionBox);
        ScrollPane scrollPane = new ScrollPane(detailBox);
        scrollPane.setFitToWidth(true);
        detailPanel.getChildren().add(scrollPane);
    }

    private void showAlbumDetail(Album album) throws IOException {
        VBox detailBox = new VBox(10);
        detailBox.setStyle("-fx-padding: 15;");

        List<Song> albumSongs = SongRepository.loadAllLocalSongs().stream()
            .filter(s -> s.getAlbum().equals(album.getName()))
            .collect(Collectors.toList());

        // Cover from first song
        if (!albumSongs.isEmpty()) {
            Song firstSong = albumSongs.get(0);
            if (firstSong.getCoverImagePath() != null && !firstSong.getCoverImagePath().isBlank()) {
                try {
                    Image img = new Image(new java.io.FileInputStream(firstSong.getCoverImagePath()));
                    detailCoverImageView.setImage(img);
                    detailBox.getChildren().add(detailCoverImageView);
                } catch (IOException ignored) {}
            }
        }

        String info = String.format(
            "Songs: %d\nGenre: %s",
            albumSongs.size(),
            albumSongs.isEmpty() ? "N/A" : albumSongs.get(0).getGenre()
        );
        detailInfoArea.setText(info);
        detailBox.getChildren().add(new Label("Info:"));
        detailBox.getChildren().add(detailInfoArea);

        // Songs list
        ObservableList<Song> songItems = FXCollections.observableArrayList(albumSongs);
        detailSongsListView.setItems(songItems);
        detailSongsListView.setPrefHeight(200);
        detailSongsListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Song selected = detailSongsListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    currentPlayQueue = new ArrayList<>(albumSongs);
                    currentPlayIndex = albumSongs.indexOf(selected);
                    playSong(selected);
                }
            }
        });
        detailBox.getChildren().add(new Label("Songs:"));
        detailBox.getChildren().add(detailSongsListView);

        // Play all button
        HBox actionBox = new HBox(10);
        actionBox.setPrefHeight(40);
        actionBox.setAlignment(Pos.CENTER);
        playButton.setText("▶ Tout écouter");
        playButton.setOnAction(e -> {
            currentPlayQueue = new ArrayList<>(albumSongs);
            if (!currentPlayQueue.isEmpty()) {
                currentPlayIndex = 0;
                playSong(currentPlayQueue.get(0));
            }
        });
        actionBox.getChildren().add(playButton);
        editButton.setDisable(true);
        deleteButton.setDisable(true);

        VBox.setVgrow(detailInfoArea, Priority.SOMETIMES);
        VBox.setVgrow(detailSongsListView, Priority.ALWAYS);
        detailBox.getChildren().add(actionBox);
        ScrollPane scrollPane = new ScrollPane(detailBox);
        scrollPane.setFitToWidth(true);
        detailPanel.getChildren().add(scrollPane);
    }

    private void showArtistDetail(String artist) throws IOException {
        VBox detailBox = new VBox(10);
        detailBox.setStyle("-fx-padding: 15;");

        List<Song> artistSongs = SongRepository.loadAllLocalSongs().stream()
            .filter(s -> s.getArtist().equals(artist))
            .collect(Collectors.toList());

        // Cover from first song
        if (!artistSongs.isEmpty()) {
            Song firstSong = artistSongs.get(0);
            if (firstSong.getCoverImagePath() != null && !firstSong.getCoverImagePath().isBlank()) {
                try {
                    Image img = new Image(new java.io.FileInputStream(firstSong.getCoverImagePath()));
                    detailCoverImageView.setImage(img);
                    detailBox.getChildren().add(detailCoverImageView);
                } catch (IOException ignored) {}
            }
        }

        String info = String.format(
            "Total Songs: %d",
            artistSongs.size()
        );
        detailInfoArea.setText(info);
        detailBox.getChildren().add(new Label("Info:"));
        detailBox.getChildren().add(detailInfoArea);

        // Songs list
        ObservableList<Song> songItems = FXCollections.observableArrayList(artistSongs);
        detailSongsListView.setItems(songItems);
        detailSongsListView.setPrefHeight(200);
        detailSongsListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Song selected = detailSongsListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    currentPlayQueue = new ArrayList<>(artistSongs);
                    currentPlayIndex = artistSongs.indexOf(selected);
                    playSong(selected);
                }
            }
        });
        detailBox.getChildren().add(new Label("Songs:"));
        detailBox.getChildren().add(detailSongsListView);

        // Play all button
        HBox actionBox = new HBox(10);
        actionBox.setPrefHeight(40);
        actionBox.setAlignment(Pos.CENTER);
        playButton.setText("▶ Tout écouter");
        playButton.setOnAction(e -> {
            currentPlayQueue = new ArrayList<>(artistSongs);
            if (!currentPlayQueue.isEmpty()) {
                currentPlayIndex = 0;
                playSong(currentPlayQueue.get(0));
            }
        });
        actionBox.getChildren().add(playButton);
        editButton.setDisable(true);
        deleteButton.setDisable(true);

        VBox.setVgrow(detailInfoArea, Priority.SOMETIMES);
        VBox.setVgrow(detailSongsListView, Priority.ALWAYS);
        detailBox.getChildren().add(actionBox);
        ScrollPane scrollPane = new ScrollPane(detailBox);
        scrollPane.setFitToWidth(true);
        detailPanel.getChildren().add(scrollPane);
    }

    private void showPlaylistDetail(Playlist playlist) throws IOException {
        VBox detailBox = new VBox(10);
        detailBox.setStyle("-fx-padding: 15;");

        List<Song> allSongs = SongRepository.loadAllLocalSongs();
        List<Song> playlistSongs = allSongs.stream()
            .filter(s -> playlist.getSongIds().contains(s.getId()))
            .collect(Collectors.toList());

        // Cover from first song
        if (!playlistSongs.isEmpty()) {
            Song firstSong = playlistSongs.get(0);
            if (firstSong.getCoverImagePath() != null && !firstSong.getCoverImagePath().isBlank()) {
                try {
                    Image img = new Image(new java.io.FileInputStream(firstSong.getCoverImagePath()));
                    detailCoverImageView.setImage(img);
                    detailBox.getChildren().add(detailCoverImageView);
                } catch (IOException ignored) {}
            }
        }

        String visibility = playlist.getVisibility() == Playlist.Visibility.PUBLIC ? "Publique" : "Privée";
        String info = String.format(
            "Propriétaire : %s\nVisibilité : %s\nMorceaux : %d",
            playlist.getOwnerUsername(),
            visibility,
            playlistSongs.size()
        );
        detailInfoArea.setText(info);
        detailBox.getChildren().add(new Label("Info:"));
        detailBox.getChildren().add(detailInfoArea);

        // Songs list
        ObservableList<Song> songItems = FXCollections.observableArrayList(playlistSongs);
        detailSongsListView.setItems(songItems);
        detailSongsListView.setPrefHeight(200);
        detailSongsListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Song selected = detailSongsListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    currentPlayQueue = new ArrayList<>(playlistSongs);
                    currentPlayIndex = playlistSongs.indexOf(selected);
                    playSong(selected);
                }
            }
        });
        detailBox.getChildren().add(new Label("Songs:"));
        detailBox.getChildren().add(detailSongsListView);

        // Play all button
        HBox actionBox = new HBox(10);
        actionBox.setPrefHeight(40);
        actionBox.setAlignment(Pos.CENTER);
        playButton.setText("▶ Tout écouter");
        playButton.setOnAction(e -> {
            currentPlayQueue = new ArrayList<>(playlistSongs);
            if (!currentPlayQueue.isEmpty()) {
                currentPlayIndex = 0;
                playSong(currentPlayQueue.get(0));
            }
        });
        actionBox.getChildren().add(playButton);

        // Edit button for owner/admin
        if (playlist.getOwnerUsername().equals(currentUser.getUsername()) ||
            currentUser.getRole() == UserRole.ADMIN) {
            editButton.setDisable(false);
            editButton.setOnAction(e -> editPlaylist(playlist));
            actionBox.getChildren().add(editButton);
            deleteButton.setDisable(true);
        } else {
            editButton.setDisable(true);
            deleteButton.setDisable(true);
        }

        VBox.setVgrow(detailInfoArea, Priority.SOMETIMES);
        VBox.setVgrow(detailSongsListView, Priority.ALWAYS);
        detailBox.getChildren().add(actionBox);
        ScrollPane scrollPane = new ScrollPane(detailBox);
        scrollPane.setFitToWidth(true);
        detailPanel.getChildren().add(scrollPane);
    }

    private void addReviewPanel(VBox parent, Song song) throws IOException {
        ReviewController reviewController = new ReviewController();
        Review currentReview = reviewController.findCurrentUserReview(song.getId(), currentUser.getId());
        ReviewController.ReviewStats stats = reviewController.getStatsForSong(song.getId());

        VBox reviewBox = new VBox(8);
        reviewBox.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-padding: 10;");

        Label reviewLabel = new Label("Reviews:");
        reviewLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");

        String statsText = String.format("👍 %d   👎 %d", stats.getLikes(), stats.getDislikes());
        reviewStatsLabel.setText(statsText);
        reviewBox.getChildren().addAll(reviewLabel, reviewStatsLabel);

        HBox reviewButtonBox = new HBox(8);
        reviewButtonBox.setAlignment(Pos.CENTER_LEFT);

        likeButton.setText(currentReview != null && currentReview.isLiked() ? "✓ Liked" : "👍 J'aime");
        likeButton.setStyle(currentReview != null && currentReview.isLiked() ?
            "-fx-text-fill: green;" : "");
        likeButton.setOnAction(e -> {
            try {
                reviewController.upsertReview(song.getId(), currentUser.getId(), true,
                    reviewCommentArea.getText());
                performSearch(); // Refresh
                showDetail(resultsListView.getSelectionModel().getSelectedItem());
            } catch (IOException | AuthException ex) {
                ex.printStackTrace();
            }
        });

        dislikeButton.setText(currentReview != null && !currentReview.isLiked() ? "✓ Disliked" : "👎 Je n'aime pas");
        dislikeButton.setStyle(currentReview != null && !currentReview.isLiked() ?
            "-fx-text-fill: red;" : "");
        dislikeButton.setOnAction(e -> {
            try {
                reviewController.upsertReview(song.getId(), currentUser.getId(), false,
                    reviewCommentArea.getText());
                performSearch(); // Refresh
                showDetail(resultsListView.getSelectionModel().getSelectedItem());
            } catch (IOException | AuthException ex) {
                ex.printStackTrace();
            }
        });

        reviewButtonBox.getChildren().addAll(likeButton, dislikeButton);
        reviewBox.getChildren().add(reviewButtonBox);

        // Display all user comments
        try {
            List<Review> allReviews = ReviewRepository.loadReviewsForSong(song.getId());
            if (!allReviews.isEmpty()) {
                Label allCommentsLabel = new Label("All comments (" + allReviews.size() + ")");
                allCommentsLabel.setStyle("-fx-font-weight: bold; -fx-padding: 8 0 0 0;");
                reviewBox.getChildren().add(allCommentsLabel);

                ListView<Review> commentsListView = new ListView<>();
                commentsListView.setItems(FXCollections.observableArrayList(allReviews));
                commentsListView.setPrefHeight(120);
                commentsListView.setCellFactory(list -> new ListCell<Review>() {
                    @Override
                    protected void updateItem(Review review, boolean empty) {
                        super.updateItem(review, empty);
                        if (empty || review == null) {
                            setText(null);
                        } else {
                            String emoji = review.isLiked() ? "👍" : "👎";
                            String text = String.format("%s %s: %s",
                                emoji,
                                review.getUserId(),
                                review.getComment() != null ? review.getComment() : "(no comment)");
                            setText(text);
                            setWrapText(true);
                        }
                    }
                });
                reviewBox.getChildren().add(commentsListView);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        reviewBox.getChildren().add(new Label(" "));
        Label yourCommentLabel = new Label("Your comment:");
        yourCommentLabel.setStyle("-fx-font-weight: bold; -fx-padding: 8 0 0 0;");
        reviewBox.getChildren().add(yourCommentLabel);

        reviewCommentArea.setPrefRowCount(3);
        reviewCommentArea.setPromptText("Add a comment (max 140 chars)...");
        if (currentReview != null) {
            reviewCommentArea.setText(currentReview.getComment());
        }

        Button saveCommentButton = new Button("Save Comment");
        saveCommentButton.setOnAction(e -> {
            try {
                String comment = reviewCommentArea.getText();
                if (comment.length() > 140) {
                    comment = comment.substring(0, 140);
                }
                boolean liked = currentReview != null ? currentReview.isLiked() : true;
                reviewController.upsertReview(song.getId(), currentUser.getId(), liked, comment);
                performSearch(); // Refresh
                showDetail(resultsListView.getSelectionModel().getSelectedItem());
            } catch (IOException | AuthException ex) {
                ex.printStackTrace();
            }
        });

        reviewBox.getChildren().addAll(
            new Label("Comment:"),
            reviewCommentArea,
            saveCommentButton
        );

        parent.getChildren().add(reviewBox);
    }

    private void playSong(Song song) {
        // Limite visiteur — compteur partagé dans App (Laksman).
        if (RoleGuard.isVisitor(App.getAuthController().getSession())) {
            if (App.getVisitorPlayCount() >= App.VISITOR_MAX_PLAYS) {
                playStatusLabel.setText(
                        "Limite visiteur atteinte : " + App.VISITOR_MAX_PLAYS
                        + " écoutes utilisées. Connectez-vous ou créez un compte pour une écoute illimitée.");
                return;
            }
            App.incrementVisitorPlayCount();
            playStatusLabel.setText("Visiteur : " + App.getVisitorPlayCount()
                    + "/" + App.VISITOR_MAX_PLAYS + " écoutes utilisées.");
        }
        try {
            if (song.getLocalStoragePath() == null || song.getLocalStoragePath().isBlank()) {
                playStatusLabel.setText("No audio file available");
                return;
            }

            // Toujours stopper le player global précédent avant de lancer un nouveau morceau.
            App.stopGlobalPlayer();

            // Arrêt et libération propre du player précédent — anti-cacophonie (Laksman).
            stopAndDisposePlayer();
            isPaused = false;

            Path audioFile = Path.of(song.getLocalStoragePath());
            if (!Files.exists(audioFile)) {
                playStatusLabel.setText("Audio file not found");
                return;
            }

            Media media = new Media(audioFile.toUri().toString());
            mediaPlayer = new MediaPlayer(media);
            App.registerPlayer(mediaPlayer); // Enregistrement global pour stopGlobalPlayer (Laksman)
            App.setNowPlaying(song);

            mediaPlayer.setOnReady(() -> {
                mediaPlayer.play();
                playStatusLabel.setText("▶ Lecture : " + song.getTitle());
            });

            mediaPlayer.setOnEndOfMedia(() -> {
                currentPlayIndex++;
                if (currentPlayIndex < currentPlayQueue.size()) {
                    playSong(currentPlayQueue.get(currentPlayIndex));
                } else {
                    currentPlayIndex = -1;
                    playStatusLabel.setText("⏹ Lecture terminée.");
                    stopAndDisposePlayer();
                }
            });
            mediaPlayer.setOnError(() ->
                playStatusLabel.setText("Erreur de lecture : " + mediaPlayer.getError()));
        } catch (Exception e) {
            playStatusLabel.setText("Error playing song: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void editSong(Song song) {
        try {
            App.setPendingSongSelectionId(song.getId());
            App.setRoot("list");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteSong(Song song) {
        if (!canManageSong(song)) {
            playStatusLabel.setText("Access denied: only the publisher or an administrator can delete this song.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete song");
        confirm.setHeaderText("Delete \"" + song.getTitle() + "\"?");
        confirm.setContentText("This will remove the music file and metadata.");

        Optional<ButtonType> decision = confirm.showAndWait();
        if (decision.isEmpty() || decision.get() != ButtonType.OK) {
            return;
        }

        try {
            SongRepository.deleteSong(song);
            AlbumRepository.removeSongFromAlbum(song.getAlbum(), song.getId());
            performSearch();
            clearDetailPanel();
            playStatusLabel.setText("Song deleted.");
        } catch (IOException ex) {
            playStatusLabel.setText("Delete failed: " + ex.getMessage());
        }
    }

    private boolean canManageSong(Song song) {
        return song != null && RoleGuard.canModify(App.getAuthController().getSession(), song.getPublisherUsername());
    }

    private void editPlaylist(Playlist playlist) {
        // Navigate to playlists and set the playlist to edit
        try {
            App.setRoot("playlist");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clearDetailPanel() {
        detailPanel.getChildren().clear();
        playStatusLabel.setText("");
    }

    @FXML
    private void goBack() throws IOException {
        App.setRoot("dashboard");
    }

}
