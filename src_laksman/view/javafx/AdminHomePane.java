package view.javafx;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import controller.AuthException;
import controller.AuthService;
import model.Role;
import model.Utilisateur;

import java.util.List;

public class AdminHomePane {
    private final AuthRouter router;
    private final AuthService authService;
    private final ListView<Utilisateur> usersList;
    private final Label feedbackLabel;

    public AdminHomePane(AuthRouter router, AuthService authService) {
        this.router = router;
        this.authService = authService;
        this.usersList = new ListView<>();
        this.feedbackLabel = new Label();
        refreshUsers();
    }

    public Parent getView() {
        Label title = new Label("Espace administrateur");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        Label description = new Label(
                "Tu peux ici gérer les comptes abonnés : suspendre, réactiver ou supprimer."
        );
        description.setWrapText(true);

        Button suspendButton = new Button("Suspendre");
        suspendButton.setOnAction(e -> suspendSelected());

        Button reactivateButton = new Button("Réactiver");
        reactivateButton.setOnAction(e -> reactivateSelected());

        Button deleteButton = new Button("Supprimer");
        deleteButton.setOnAction(e -> deleteSelected());

        Button refreshButton = new Button("Rafraîchir");
        refreshButton.setOnAction(e -> refreshUsers());

        Button logoutButton = new Button("Se déconnecter");
        logoutButton.setOnAction(e -> router.logoutToMain());

        HBox actions = new HBox(10, suspendButton, reactivateButton, deleteButton, refreshButton, logoutButton);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setTop(new VBox(10, title, description));
        root.setCenter(usersList);
        root.setBottom(new VBox(10, actions, feedbackLabel));
        BorderPane.setMargin(usersList, new Insets(16, 0, 16, 0));
        return root;
    }

    private void suspendSelected() {
        Utilisateur utilisateur = usersList.getSelectionModel().getSelectedItem();
        if (utilisateur == null) {
            feedbackLabel.setText("Sélectionne un utilisateur.");
            return;
        }
        try {
            authService.suspendreCompte(utilisateur.getNomUtilisateur());
            feedbackLabel.setText("Compte suspendu : " + utilisateur.getNomUtilisateur());
            refreshUsers();
        } catch (AuthException e) {
            feedbackLabel.setText(e.getMessage());
        }
    }

    private void reactivateSelected() {
        Utilisateur utilisateur = usersList.getSelectionModel().getSelectedItem();
        if (utilisateur == null) {
            feedbackLabel.setText("Sélectionne un utilisateur.");
            return;
        }
        try {
            authService.reactiverCompte(utilisateur.getNomUtilisateur());
            feedbackLabel.setText("Compte réactivé : " + utilisateur.getNomUtilisateur());
            refreshUsers();
        } catch (AuthException e) {
            feedbackLabel.setText(e.getMessage());
        }
    }

    private void deleteSelected() {
        Utilisateur utilisateur = usersList.getSelectionModel().getSelectedItem();
        if (utilisateur == null) {
            feedbackLabel.setText("Sélectionne un utilisateur.");
            return;
        }
        try {
            authService.supprimerCompte(utilisateur.getNomUtilisateur());
            feedbackLabel.setText("Compte supprimé : " + utilisateur.getNomUtilisateur());
            refreshUsers();
        } catch (AuthException e) {
            feedbackLabel.setText(e.getMessage());
        }
    }

    private void refreshUsers() {
        List<Utilisateur> utilisateurs = authService.listerUtilisateurs().stream()
                .filter(u -> u.getRole() != Role.VISITEUR)
                .toList();
        usersList.setItems(FXCollections.observableArrayList(utilisateurs));
        usersList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Utilisateur item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getNomUtilisateur() + " [" + item.getRole() + "] actif=" + item.isActif());
                }
            }
        });
    }
}
