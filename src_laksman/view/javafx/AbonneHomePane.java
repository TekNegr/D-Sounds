package view.javafx;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import controller.AuthService;
import model.CataloguePermissions;
import model.Utilisateur;

public class AbonneHomePane {
    private final AuthRouter router;
    private final AuthService authService;
    private final Utilisateur utilisateur;

    public AbonneHomePane(AuthRouter router, AuthService authService, Utilisateur utilisateur) {
        this.router = router;
        this.authService = authService;
        this.utilisateur = utilisateur;
    }

    public Parent getView() {
        Label title = new Label("Espace abonné");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        Label info = new Label("Connecté en tant que : " + utilisateur.getNomUtilisateur());
        Label permissions = new Label(
                "Peut créer des playlists : " +
                        CataloguePermissions.peutCreerPlaylist(authService.getSession()) +
                        "\nPeut gérer le catalogue : " +
                        CataloguePermissions.peutGererCatalogue(authService.getSession())
        );
        permissions.setWrapText(true);

        Label nextStep = new Label(
                "Point de branchement prêt pour l'équipe : playlists, historique d'écoute, recommandations."
        );
        nextStep.setWrapText(true);

        Button logoutButton = new Button("Se déconnecter");
        logoutButton.setOnAction(e -> router.logoutToMain());

        VBox root = new VBox(16, title, info, permissions, nextStep, logoutButton);
        root.setPadding(new Insets(20));
        return root;
    }
}
