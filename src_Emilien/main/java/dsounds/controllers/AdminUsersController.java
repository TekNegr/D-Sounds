package dsounds.controllers;

import dsounds.App;
import dsounds.models.User;
import dsounds.models.UserRole;

import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

public class AdminUsersController {

    @FXML private ListView<String> userListView;
    @FXML private Label statusLabel;
    @FXML private VBox detailPane;
    @FXML private Label detailUsername;
    @FXML private Label detailEmail;
    @FXML private Label detailRole;
    @FXML private Label detailStatus;
    @FXML private Label detailCreated;
    @FXML private Button btnSuspend;
    @FXML private Button btnReactivate;
    @FXML private Button btnDelete;

    private List<User> users;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    .withZone(ZoneId.systemDefault());

    @FXML
    private void initialize() {
        detailPane.setVisible(false);
        refreshList();

        userListView.getSelectionModel().selectedIndexProperty()
                .addListener((obs, oldVal, newVal) -> {
                    int idx = newVal.intValue();
                    if (idx >= 0 && idx < users.size()) {
                        showUserDetails(users.get(idx));
                    }
                });
    }

    @FXML
    private void refreshList() {
        users = App.getAuthController().listUsers();
        userListView.setItems(FXCollections.observableArrayList(
                users.stream()
                        .map(u -> u.getUsername()
                                + " [" + u.getRole() + "]"
                                + (u.isActive() ? "" : " (suspended)"))
                        .collect(Collectors.toList())
        ));
        detailPane.setVisible(false);
        statusLabel.setText("Users loaded: " + users.size());
    }

    private void showUserDetails(User user) {
        detailPane.setVisible(true);
        detailUsername.setText("Username: " + user.getUsername());
        detailEmail.setText("Email: " + user.getEmail());
        detailRole.setText("Role: " + user.getRole());
        detailStatus.setText("Status: " + (user.isActive() ? "Active" : "Suspended"));
        detailCreated.setText("Created: " + DATE_FMT.format(user.getCreatedAt()));

        boolean isAdmin = user.getRole() == UserRole.ADMIN;
        btnSuspend.setVisible(!isAdmin && user.isActive());
        btnSuspend.setManaged(!isAdmin && user.isActive());
        btnReactivate.setVisible(!isAdmin && !user.isActive());
        btnReactivate.setManaged(!isAdmin && !user.isActive());
        btnDelete.setVisible(!isAdmin);
        btnDelete.setManaged(!isAdmin);
    }

    @FXML
    private void suspendUser() {
        User selected = getSelectedUser();
        if (selected == null) return;
        try {
            App.getAuthController().suspendUser(selected.getUsername());
            statusLabel.setText("User " + selected.getUsername() + " suspended.");
            statusLabel.setStyle("-fx-text-fill: orange;");
            refreshList();
        } catch (AuthException ex) {
            statusLabel.setText(ex.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void reactivateUser() {
        User selected = getSelectedUser();
        if (selected == null) return;
        try {
            App.getAuthController().reactivateUser(selected.getUsername());
            statusLabel.setText("User " + selected.getUsername() + " reactivated.");
            statusLabel.setStyle("-fx-text-fill: green;");
            refreshList();
        } catch (AuthException ex) {
            statusLabel.setText(ex.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void deleteUser() {
        User selected = getSelectedUser();
        if (selected == null) return;
        try {
            App.getAuthController().deleteUser(selected.getUsername());
            statusLabel.setText("User " + selected.getUsername() + " deleted.");
            statusLabel.setStyle("-fx-text-fill: green;");
            refreshList();
        } catch (AuthException ex) {
            statusLabel.setText(ex.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void backToDashboard() throws IOException {
        App.setRoot("dashboard");
    }

    private User getSelectedUser() {
        int idx = userListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= users.size()) {
            statusLabel.setText("Please select a user first.");
            statusLabel.setStyle("-fx-text-fill: red;");
            return null;
        }
        return users.get(idx);
    }
}