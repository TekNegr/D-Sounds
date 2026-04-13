package view.javafx;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import controller.AuthException;
import model.Utilisateur;

public class SignupPane {
    private final AuthRouter router;
    private final VBox view;
    private final TextField usernameField;
    private final PasswordField passwordField;
    private final PasswordField confirmPasswordField;
    private final Label feedbackLabel;

    public SignupPane(AuthRouter router) {
        this.router = router;
        this.usernameField = new TextField();
        this.passwordField = new PasswordField();
        this.confirmPasswordField = new PasswordField();
        this.feedbackLabel = new Label();
        this.view = buildView();
    }

    public Parent getView() {
        return view;
    }

    private VBox buildView() {
        Label title = new Label("Créer un compte abonné");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        usernameField.setPromptText("Choisis un nom d'utilisateur");
        passwordField.setPromptText("Mot de passe");
        confirmPasswordField.setPromptText("Confirme le mot de passe");

        Button signupButton = new Button("Créer le compte");
        signupButton.setMaxWidth(Double.MAX_VALUE);
        signupButton.setOnAction(event -> handleSignup());

        feedbackLabel.setWrapText(true);

        VBox box = new VBox(12,
                title,
                new Label("Identifiant"), usernameField,
                new Label("Mot de passe"), passwordField,
                new Label("Confirmation"), confirmPasswordField,
                signupButton,
                feedbackLabel
        );
        box.setPadding(new Insets(20));
        box.setPrefWidth(320);
        return box;
    }

    private void handleSignup() {
        try {
            if (!passwordField.getText().equals(confirmPasswordField.getText())) {
                throw new AuthException("Les mots de passe ne correspondent pas.");
            }
            Utilisateur utilisateur = router.signup(usernameField.getText(), passwordField.getText());
            feedbackLabel.setText("Compte créé : " + utilisateur.getNomUtilisateur() + ". Connecte-toi maintenant.");
            usernameField.clear();
            passwordField.clear();
            confirmPasswordField.clear();
        } catch (AuthException e) {
            feedbackLabel.setText("Création impossible : " + e.getMessage());
        }
    }
}
