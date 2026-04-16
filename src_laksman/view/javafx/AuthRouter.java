package view.javafx;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import integration.oauth.GitHubOAuthService;
import integration.oauth.GoogleOAuthService;
import integration.oauth.LinkedOAuthAccount;
import integration.oauth.OAuthDesktopConfig;
import integration.oauth.OAuthException;
import controller.AuthException;
import controller.AuthService;
import model.Role;
import model.Utilisateur;

public class AuthRouter {
    private final Stage stage;
    private final AuthService authService;

    public AuthRouter(Stage stage, AuthService authService) {
        this.stage = stage;
        this.authService = authService;
    }

    public Scene buildMainScene() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        Label appTitle = new Label("MusicApp - Authentification");
        appTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Label subtitle = new Label("Connexion, inscription, rôles, session visiteur et gestion des comptes abonnés.");
        subtitle.setWrapText(true);

        VBox left = new VBox(12, appTitle, subtitle, new LoginPane(this).getView());
        VBox right = new VBox(12, new SignupPane(this).getView());
        left.setPadding(new Insets(10));
        right.setPadding(new Insets(10));

        root.setLeft(left);
        root.setCenter(right);
        return new Scene(root, 900, 560);
    }

    public void showHome(Utilisateur utilisateur) {
        Scene scene = switch (utilisateur.getRole()) {
            case ADMIN -> new Scene(new AdminHomePane(this, authService).getView(), 960, 620);
            case ABONNE -> new Scene(new AbonneHomePane(this, authService, utilisateur).getView(), 960, 620);
            case VISITEUR -> new Scene(new VisiteurHomePane(this, authService, utilisateur).getView(), 960, 620);
        };
        stage.setScene(scene);
    }

    public void continueAsVisitor() {
        authService.continuerCommeVisiteur();
        showHome(authService.getSession().getUtilisateurCourant());
    }

    public Utilisateur login(String username, String password, Role expectedRole) throws AuthException {
        return authService.connecter(username, password, expectedRole);
    }

    public Utilisateur signup(String username, String password) throws AuthException {
        return authService.inscrireAbonne(username, password);
    }



    public Utilisateur loginWithGoogle() throws AuthException {
        try {
            OAuthDesktopConfig config = OAuthDesktopConfig.fromSystemProperties("google.oauth", 8751);
            LinkedOAuthAccount account = new GoogleOAuthService(config).authenticate();
            String identifiant = extraireIdentifiantOAuth(account);
            return authService.connecterOuCreerDepuisOAuth(identifiant);
        } catch (OAuthException e) {
            throw new AuthException("Google OAuth : " + e.getMessage());
        }
    }

    public Utilisateur loginWithGitHub() throws AuthException {
        try {
            OAuthDesktopConfig config = OAuthDesktopConfig.fromSystemProperties("github.oauth", 8752);
            LinkedOAuthAccount account = new GitHubOAuthService(config).authenticate();
            String identifiant = extraireIdentifiantOAuth(account);
            return authService.connecterOuCreerDepuisOAuth(identifiant);
        } catch (OAuthException e) {
            throw new AuthException("GitHub OAuth : " + e.getMessage());
        }
    }

    private String extraireIdentifiantOAuth(LinkedOAuthAccount account) throws AuthException {
        if (account == null || account.getProfile() == null) {
            throw new AuthException("Profil OAuth introuvable.");
        }
        String email = account.getProfile().getEmail();
        if (email != null && !email.isBlank()) {
            int arobase = email.indexOf('@');
            return arobase > 0 ? email.substring(0, arobase) : email;
        }
        String displayName = account.getProfile().getDisplayName();
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }
        throw new AuthException("Impossible de déduire un identifiant local depuis OAuth.");
    }

    public void logoutToMain() {
        authService.deconnecter();
        stage.setScene(buildMainScene());
    }

    public AuthService getAuthService() {
        return authService;
    }
}
