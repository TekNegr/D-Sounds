package dsounds.controllers;

import dsounds.App;
import dsounds.models.User;
import dsounds.models.UserRole;
import dsounds.security.RoleGuard;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * DashboardController — écran principal après connexion.
 *
 * <p>Les boutons sont activés/désactivés selon le rôle de l'utilisateur connecté,
 * via {@link RoleGuard}. Cela constitue la première couche de restriction UI.</p>
 *
 * <p><b>Modifié par Laksman</b> — restriction des boutons par rôle.</p>
 */
public class DashboardController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Button artistUploadButton;

    @FXML
    private Button adminPanelButton;

    @FXML
    private Button playlistButton;

    @FXML
    private Label roleInfoLabel;

    @FXML
    private void initialize() {
        User currentUser = App.getAuthController().getSession().getCurrentUser();
        if (currentUser != null) {
            welcomeLabel.setText("Bienvenue, " + currentUser.getUsername()
                    + " (" + currentUser.getRole() + ")");
        }

        applyRoleRestrictions();
    }

    /**
     * Applique les restrictions de boutons selon le rôle de l'utilisateur.
     *
     * <ul>
     *   <li>VISITOR : ne peut pas créer de playlists ni uploader.</li>
     *   <li>SUBSCRIBER : peut créer des playlists, ne peut pas uploader
     *       (réservé aux artistes/admins).</li>
     *   <li>ADMIN : accès complet.</li>
     * </ul>
     *
     * <b>Laksman</b> — restriction UI par rôle.
     */
    private void applyRoleRestrictions() {
        var session = App.getAuthController().getSession();

        // Le panel admin est réservé aux admins (Laksman).
        if (adminPanelButton != null) {
            adminPanelButton.setDisable(!RoleGuard.canManageUsers(session));
            adminPanelButton.setOpacity(RoleGuard.canManageUsers(session) ? 1.0 : 0.45);
        }

        // L'upload de morceaux est réservé aux admins dans cette version.
        if (artistUploadButton != null) {
            artistUploadButton.setDisable(!RoleGuard.canManageCatalog(session));
            artistUploadButton.setOpacity(RoleGuard.canManageCatalog(session) ? 1.0 : 0.45);
        }

        // Les playlists sont réservées aux abonnés et admins.
        if (playlistButton != null) {
            playlistButton.setDisable(!RoleGuard.canCreatePlaylist(session));
            playlistButton.setOpacity(RoleGuard.canCreatePlaylist(session) ? 1.0 : 0.45);
        }

        // Message d'information sur le rôle.
        if (roleInfoLabel != null) {
            if (RoleGuard.isVisitor(session)) {
                roleInfoLabel.setText(
                        "Visitor mode: catalogue browsing only. "
                      + "Log in or create an account to access playlists.");
            } else if (RoleGuard.isAdmin(session)) {
                roleInfoLabel.setText("Administrateur : accès complet.");
            } else {
                roleInfoLabel.setText("Abonné : playlists et écoute illimitée activées.");
            }
        }
    }

    @FXML
    private void switchToBrowser() throws IOException {
        App.setRoot("browser");
    }

    @FXML
    private void switchToArtist() throws IOException {
        // Double vérification côté contrôleur (Laksman — défense en profondeur).
        if (!RoleGuard.canManageCatalog(App.getAuthController().getSession())) {
            return; // Ne rien faire si désactivé.
        }
        App.setRoot("artist");
    }

    @FXML
    private void switchToList() throws IOException {
        App.setRoot("list");
    }

    @FXML
    private void switchToPlaylists() throws IOException {
        App.setRoot("playlist");
    }

    @FXML
    private void switchToAdmin() throws IOException {
        // Double vérification côté contrôleur (Laksman — défense en profondeur).
        if (!RoleGuard.canManageUsers(App.getAuthController().getSession())) {
            return;
        }
        App.setRoot("admin");
    }

    @FXML
    private void logout() throws IOException {
        App.stopGlobalPlayer(); // Stopper la musique avant de déconnecter (Laksman).
        App.getAuthController().logout();
        App.resetVisitorPlayCount();
        App.setRoot("auth");
    }
}
