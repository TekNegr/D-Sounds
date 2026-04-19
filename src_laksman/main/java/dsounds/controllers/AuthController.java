package dsounds.controllers;

import dsounds.App;
import dsounds.models.User;
import dsounds.models.UserRole;
import dsounds.security.RoleGuard;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * AuthController — gestion de la connexion, l'inscription et l'accès visiteur.
 *
 * <p>Après connexion, redirige vers le dashboard. Le rôle de l'utilisateur est
 * affiché dans le message de statut. Le {@link dsounds.controllers.DashboardController}
 * se charge ensuite d'activer/désactiver les boutons selon le rôle via
 * {@link RoleGuard}.</p>
 *
 * <p><b>Modifié par Laksman</b> — affichage du rôle, info sur les restrictions visiteur.</p>
 */
public class AuthController {

    @FXML
    private TextField loginUsernameField;

    @FXML
    private PasswordField loginPasswordField;

    @FXML
    private TextField registerUsernameField;

    @FXML
    private TextField registerEmailField;

    @FXML
    private PasswordField registerPasswordField;

    @FXML
    private Label statusLabel;

    @FXML
    private void initialize() {
        statusLabel.setText(
                "Utilisez admin / root pour vous connecter en tant qu'administrateur. "
              + "Les visiteurs ont un accès limité (pas de playlists, lecture seule).");
    }

    @FXML
    private void login() {
        try {
            User user = App.getAuthController().login(
                    loginUsernameField.getText(),
                    loginPasswordField.getText()
            );

            // Message informatif sur le rôle (Laksman).
            String roleInfo = buildRoleInfo(user);
            statusLabel.setText("Bienvenue, " + user.getUsername()
                    + " (" + user.getRole() + "). " + roleInfo);

            App.resetVisitorPlayCount(); // Réinitialise le compteur d'écoutes visiteur (Laksman).
            App.setRoot("dashboard");
        } catch (AuthException | IOException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void register() {
        try {
            User user = App.getAuthController().registerSubscriber(
                    registerUsernameField.getText(),
                    registerEmailField.getText(),
                    registerPasswordField.getText()
            );
            statusLabel.setText("Compte créé pour " + user.getUsername()
                    + " [ABONNÉ]. Vous pouvez maintenant vous connecter.");
            registerPasswordField.clear();
        } catch (AuthException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void continueAsVisitor() {
        try {
            App.getAuthController().continueAsVisitor();
            statusLabel.setText(
                    "Connecté en tant que visiteur. "
                  + "Note : les playlists et l'upload sont désactivés en mode visiteur.");
            App.resetVisitorPlayCount(); // Nouvelle session visiteur — compteur remis à zéro (Laksman).
            App.setRoot("dashboard");
        } catch (IOException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }

    /**
     * Génère un message informatif sur les droits du rôle (Laksman).
     */
    private static String buildRoleInfo(User user) {
        if (user == null) {
            return "";
        }
        return switch (user.getRole()) {
            case ADMIN      -> "Full access: catalogue management and user administration enabled.";
            case SUBSCRIBER -> "Playlists and unlimited listening enabled.";
            case VISITOR    -> "Limited access: catalogue only, no playlists.";
        };
    }
}
