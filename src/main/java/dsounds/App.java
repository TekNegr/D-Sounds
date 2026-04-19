package dsounds;

import dsounds.controllers.LocalAuthController;

import javafx.application.Application;
import javafx.scene.media.MediaPlayer;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

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

    /**
     * Enregistre le player actif — à appeler après chaque new MediaPlayer().
     */
    public static void registerPlayer(MediaPlayer player) {
        globalPlayer = player;
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
        scene.setRoot(loadFXML(fxml));
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
