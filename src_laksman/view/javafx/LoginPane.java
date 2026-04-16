package view.javafx;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import controller.AuthException;
import model.Role;
import model.Utilisateur;

public class LoginPane {
    private final AuthRouter router;
    private final VBox view;
    private final TextField usernameField;
    private final PasswordField passwordField;
    private final ComboBox<Role> roleCombo;
    private final Label feedbackLabel;

    public LoginPane(AuthRouter router) {
        this.router = router;
        this.usernameField = new TextField();
        this.passwordField = new PasswordField();
        this.roleCombo = new ComboBox<>();
        this.feedbackLabel = new Label();
        this.view = buildView();
    }

    public Parent getView() {
        return view;
    }

    private VBox buildView() {
        Label title = new Label("Connexion");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        usernameField.setPromptText("Nom d'utilisateur");
        passwordField.setPromptText("Mot de passe");
        roleCombo.getItems().addAll(Role.ADMIN, Role.ABONNE);
        roleCombo.setValue(Role.ABONNE);

        Button loginButton = new Button("Se connecter");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(event -> handleLogin());

        Button googleButton = new Button("Se connecter avec Google");
        googleButton.setMaxWidth(Double.MAX_VALUE);
        googleButton.setOnAction(event -> handleGoogleLogin());

        Button githubButton = new Button("Se connecter avec GitHub");
        githubButton.setMaxWidth(Double.MAX_VALUE);
        githubButton.setOnAction(event -> handleGitHubLogin());

        Button visitorButton = new Button("Continuer en visiteur");
        visitorButton.setMaxWidth(Double.MAX_VALUE);
        visitorButton.setOnAction(event -> {
            feedbackLabel.setText("");
            router.continueAsVisitor();
        });

        feedbackLabel.setWrapText(true);

        VBox box = new VBox(12,
                title,
                new Label("Type d'accès"), roleCombo,
                new Label("Identifiant"), usernameField,
                new Label("Mot de passe"), passwordField,
                loginButton,
                googleButton,
                githubButton,
                visitorButton,
                feedbackLabel
        );
        box.setPadding(new Insets(20));
        box.setPrefWidth(320);
        return box;
    }

    private void handleLogin() {
        try {
            Utilisateur utilisateur = router.login(
                    usernameField.getText(),
                    passwordField.getText(),
                    roleCombo.getValue()
            );
            feedbackLabel.setText("Connexion réussie.");
            passwordField.clear();
            router.showHome(utilisateur);
        } catch (AuthException e) {
            feedbackLabel.setText("Connexion refusée : " + e.getMessage());
        }
    }


    private void handleGoogleLogin() {
        try {
            Utilisateur utilisateur = router.loginWithGoogle();
            feedbackLabel.setText("Connexion Google réussie.");
            router.showHome(utilisateur);
        } catch (AuthException e) {
            feedbackLabel.setText("Connexion Google refusée : " + e.getMessage());
        }
    }

    private void handleGitHubLogin() {
        try {
            Utilisateur utilisateur = router.loginWithGitHub();
            feedbackLabel.setText("Connexion GitHub réussie.");
            router.showHome(utilisateur);
        } catch (AuthException e) {
            feedbackLabel.setText("Connexion GitHub refusée : " + e.getMessage());
        }
    }

}
