package dsounds;

import dsounds.controllers.LocalAuthController;
import dsounds.models.Song;
import dsounds.repositories.SongRepository;

import javafx.application.Application;
import javafx.scene.layout.BorderPane;
import javafx.scene.media.MediaPlayer;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Point d'entrée principal de l'application dSounds.
 *
 * <p>Contient le compteur d'écoutes partagé pour les visiteurs (Laksman).
 * Ce compteur est global à la session — il s'applique sur tous les écrans
 * (navigateur musical ET bibliothèque), ce qui empêche un visiteur de
 * contourner la limite de 5 écoutes en changeant d'écran.</p>
 */
public class App extends Application {

    private static Scene scene;
    private static LocalAuthController authController;
    private static String pendingSongSelectionId;

    /**
     * Référence globale au MediaPlayer actif.
     * Permet de le stopper depuis n'importe quel contrôleur (ex: logout) (Laksman).
     */
    private static MediaPlayer globalPlayer;
    private static boolean globalPaused;
    private static String nowPlayingSongId;
    private static String nowPlayingTitle;
    private static String nowPlayingArtist;

    /**
     * Enregistre le player actif — à appeler après chaque new MediaPlayer().
     */
    public static void registerPlayer(MediaPlayer player) {
        globalPlayer = player;
        globalPaused = false;
    }

    /**
     * Stoppe et libère le player actif — à appeler au logout et changement d'écran (Laksman).
     */
    public static void stopGlobalPlayer() {
        if (globalPlayer != null) {
            try {
                globalPlayer.stop();
                globalPlayer.dispose();
            } catch (Exception ignored) {}
            globalPlayer = null;
        }
        globalPaused = false;
        nowPlayingSongId = null;
        nowPlayingTitle = null;
        nowPlayingArtist = null;
    }

    public static void setNowPlaying(Song song) {
        if (song == null) {
            nowPlayingSongId = null;
            nowPlayingTitle = null;
            nowPlayingArtist = null;
            return;
        }
        nowPlayingSongId = song.getId();
        nowPlayingTitle = song.getTitle();
        nowPlayingArtist = song.getArtist();
    }

    public static String getNowPlayingTitle() {
        return nowPlayingTitle;
    }

    public static String getNowPlayingArtist() {
        return nowPlayingArtist;
    }

    public static boolean hasGlobalPlayer() {
        return globalPlayer != null;
    }

    public static boolean isGlobalPaused() {
        return globalPaused;
    }

    public static void toggleGlobalPause() {
        if (globalPlayer == null) {
            return;
        }
        if (globalPaused) {
            globalPlayer.play();
            globalPaused = false;
        } else {
            globalPlayer.pause();
            globalPaused = true;
        }
    }

    public static void playNextGlobal() {
        playByOffset(1);
    }

    public static void playPreviousGlobal() {
        playByOffset(-1);
    }

    private static void playByOffset(int offset) {
        try {
            List<Song> songs = SongRepository.loadAllLocalSongs();
            if (songs.isEmpty()) {
                return;
            }

            int currentIndex = -1;
            if (nowPlayingSongId != null) {
                for (int i = 0; i < songs.size(); i++) {
                    if (nowPlayingSongId.equals(songs.get(i).getId())) {
                        currentIndex = i;
                        break;
                    }
                }
            }

            int nextIndex;
            if (currentIndex < 0) {
                nextIndex = offset >= 0 ? 0 : songs.size() - 1;
            } else {
                nextIndex = currentIndex + offset;
                if (nextIndex < 0) {
                    nextIndex = songs.size() - 1;
                } else if (nextIndex >= songs.size()) {
                    nextIndex = 0;
                }
            }

            playGlobalSong(songs.get(nextIndex));
        } catch (IOException ignored) {
            // Keep UI responsive if metadata cannot be read.
        }
    }

    private static void playGlobalSong(Song song) {
        if (song == null) {
            return;
        }

        Path audioPath = resolveSongPath(song);
        if (audioPath == null || !Files.exists(audioPath)) {
            return;
        }

        stopGlobalPlayer();

        javafx.scene.media.Media media = new javafx.scene.media.Media(audioPath.toUri().toString());
        MediaPlayer player = new MediaPlayer(media);
        registerPlayer(player);
        setNowPlaying(song);
        player.setOnEndOfMedia(App::playNextGlobal);
        player.play();
    }

    private static Path resolveSongPath(Song song) {
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
     * Compteur d'écoutes partagé pour les visiteurs.
     * Remis à zéro à chaque connexion/déconnexion via resetVisitorPlayCount().
     * Utilisé par BrowserController et ListController (Laksman).
     */
    private static int visitorPlayCount = 0;

    /** Nombre maximum d'écoutes autorisées par session pour un visiteur. */
    public static final int VISITOR_MAX_PLAYS = 5;

    @Override
    public void start(Stage stage) throws IOException {
        authController = LocalAuthController.createDefault();
        try {
            authController.loadUsers();
        } catch (ClassNotFoundException ex) {
            throw new IOException("Impossible de charger le stockage utilisateurs.", ex);
        }

        scene = new Scene(loadFXML("auth"), 760, 560);
        scene.getStylesheets().add(App.class.getResource("style.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("dSounds");
        stage.show();
    }

    @Override
    public void stop() throws IOException {
        // Sauvegarde automatique à la fermeture (Laksman).
        // Les playlists sont persistées en temps réel par PlaylistRepository (fichiers .properties).
        // Seuls les utilisateurs nécessitent une sauvegarde explicite ici.
        if (authController != null) {
            authController.saveUsers();
        }
    }

    public static void setRoot(String fxml) throws IOException {
        Parent pageRoot = loadFXML(fxml);
        if (shouldUseGlobalShell(fxml)) {
            BorderPane shell = new BorderPane();
            shell.setTop(loadFXML("global_bar"));
            shell.setCenter(pageRoot);
            scene.setRoot(shell);
        } else {
            scene.setRoot(pageRoot);
        }
    }

    private static boolean shouldUseGlobalShell(String fxml) {
        if ("auth".equals(fxml)) {
            return false;
        }
        return authController != null
                && authController.getSession() != null
                && authController.getSession().isAuthenticated();
    }

    public static LocalAuthController getAuthController() {
        return authController;
    }

    public static void setPendingSongSelectionId(String songId) {
        pendingSongSelectionId = songId;
    }

    public static String consumePendingSongSelectionId() {
        String songId = pendingSongSelectionId;
        pendingSongSelectionId = null;
        return songId;
    }

    // -------------------------------------------------------------------------
    // Compteur d'écoutes visiteur partagé (Laksman)
    // -------------------------------------------------------------------------

    /**
     * Retourne le nombre d'écoutes effectuées par le visiteur cette session.
     */
    public static int getVisitorPlayCount() {
        return visitorPlayCount;
    }

    /**
     * Incrémente le compteur d'écoutes du visiteur.
     */
    public static void incrementVisitorPlayCount() {
        visitorPlayCount++;
    }

    /**
     * Remet le compteur à zéro — à appeler lors de chaque connexion/déconnexion.
     */
    public static void resetVisitorPlayCount() {
        visitorPlayCount = 0;
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}
