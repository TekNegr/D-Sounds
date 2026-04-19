package dsounds.controllers;

import dsounds.App;
import dsounds.models.User;
import dsounds.models.UserRole;
import dsounds.security.RoleGuard;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

/**
 * AdminController — écran de gestion des comptes utilisateurs.
 *
 * <p>Accessible uniquement aux administrateurs (vérifié à l'ouverture via
 * {@link RoleGuard#requireAdmin} et à chaque action).</p>
 *
 * <p>Fonctionnalités :</p>
 * <ul>
 *   <li>Liste de tous les comptes (admins + abonnés) avec statut actif/suspendu.</li>
 *   <li>Suspendre un compte abonné.</li>
 *   <li>Réactiver un compte suspendu.</li>
 *   <li>Supprimer un compte (avec confirmation).</li>
 *   <li>Statistiques simples : nb utilisateurs, actifs, suspendus.</li>
 * </ul>
 *
 * <p><b>Auteur (Laksman)</b> — gestion des comptes et restrictions d'accès admin.</p>
 */
public class AdminController {

    @FXML
    private ListView<User> usersListView;

    @FXML
    private Label statsLabel;

    @FXML
    private Label feedbackLabel;

    @FXML
    private Button suspendButton;

    @FXML
    private Button reactivateButton;

    @FXML
    private Button deleteButton;

    @FXML
    private void initialize() {
        // Vérification de rôle à l'initialisation — redirige si pas admin.
        try {
            RoleGuard.requireAdmin(App.getAuthController().getSession());
        } catch (AuthException e) {
            try {
                feedbackLabel.setText("Accès refusé : administrateurs uniquement.");
                App.setRoot("dashboard");
            } catch (IOException ignored) {}
            return;
        }

        configureListView();
        refreshAll();
    }

    @FXML
    private void suspendSelected() {
        User target = usersListView.getSelectionModel().getSelectedItem();
        if (target == null) {
            feedbackLabel.setText("Sélectionnez d'abord un utilisateur.");
            return;
        }
        try {
            // RoleGuard vérifié dans LocalAuthController (défense en profondeur).
            App.getAuthController().suspendUser(target.getUsername());
            feedbackLabel.setText("Compte suspendu : " + target.getUsername());
            refreshAll();
        } catch (AuthException e) {
            feedbackLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void reactivateSelected() {
        User target = usersListView.getSelectionModel().getSelectedItem();
        if (target == null) {
            feedbackLabel.setText("Sélectionnez d'abord un utilisateur.");
            return;
        }
        try {
            App.getAuthController().reactivateUser(target.getUsername());
            feedbackLabel.setText("Compte réactivé : " + target.getUsername());
            refreshAll();
        } catch (AuthException e) {
            feedbackLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void deleteSelected() {
        User target = usersListView.getSelectionModel().getSelectedItem();
        if (target == null) {
            feedbackLabel.setText("Sélectionnez d'abord un utilisateur.");
            return;
        }

        // Confirmation avant suppression définitive.
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer définitivement le compte \"" + target.getUsername() + "\"?\nCette action est irréversible.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmer la suppression");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    App.getAuthController().deleteUser(target.getUsername());
                    feedbackLabel.setText("Compte supprimé : " + target.getUsername());
                    refreshAll();
                } catch (AuthException e) {
                    feedbackLabel.setText(e.getMessage());
                }
            }
        });
    }

    @FXML
    private void refreshUsers() {
        refreshAll();
        feedbackLabel.setText("Liste actualisée.");
    }

    @FXML
    private void switchToDashboard() throws IOException {
        App.setRoot("dashboard");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void refreshAll() {
        List<User> users = App.getAuthController().listUsers().stream()
                .filter(u -> u.getRole() != UserRole.VISITOR)
                .collect(Collectors.toList());
        usersListView.setItems(FXCollections.observableArrayList(users));

        long total     = users.size();
        long admins    = users.stream().filter(u -> u.getRole() == UserRole.ADMIN).count();
        long subs      = users.stream().filter(u -> u.getRole() == UserRole.SUBSCRIBER).count();
        long active    = users.stream().filter(User::isActive).count();
        long suspended = total - active;

        statsLabel.setText(String.format(
                "Total : %d  |  Admins : %d  |  Abonnés : %d  |  Actifs : %d  |  Suspendus : %d",
                total, admins, subs, active, suspended));
    }

    private void configureListView() {
        usersListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    String status = item.isActive() ? "✅ actif" : "⛔ suspendu";
                    setText(String.format("%-22s [%-10s]  %s",
                            item.getUsername(), item.getRole(), status));
                    setStyle(item.isActive()
                            ? "-fx-text-fill: #2c3e50;"
                            : "-fx-text-fill: #c0392b; -fx-font-style: italic;");
                }
            }
        });

        // Activer/désactiver les boutons selon la sélection.
        usersListView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            boolean hasSelection = selected != null;
            boolean isAdminAccount = hasSelection && selected.getRole() == UserRole.ADMIN;

            suspendButton.setDisable(!hasSelection || isAdminAccount || !selected.isActive());
            reactivateButton.setDisable(!hasSelection || isAdminAccount || selected.isActive());
            deleteButton.setDisable(!hasSelection || isAdminAccount);
        });
    }
}
