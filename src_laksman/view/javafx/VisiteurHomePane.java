package view.javafx;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import controller.AuthService;
import model.CataloguePermissions;
import model.Utilisateur;

public class VisiteurHomePane {
    private final AuthRouter router;
    private final AuthService authService;
    private final Utilisateur utilisateur;

    public VisiteurHomePane(AuthRouter router, AuthService authService, Utilisateur utilisateur) {
        this.router = router;
        this.authService = authService;
        this.utilisateur = utilisateur;
    }

    public Parent getView() {
        Label title = new Label("Mode visiteur");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        Label info = new Label("Session temporaire : " + utilisateur.getNomUtilisateur());
        Label permissions = new Label(
                "Peut consulter le catalogue : " +
                        CataloguePermissions.peutConsulterCatalogue(authService.getSession()) +
                        "\nPeut créer des playlists : " +
                        CataloguePermissions.peutCreerPlaylist(authService.getSession())
        );
        permissions.setWrapText(true);

        Label nextStep = new Label(
                "Point de branchement prêt pour l'équipe : catalogue visiteur et limitation du nombre d'écoutes."
        );
        nextStep.setWrapText(true);

        Button logoutButton = new Button("Retour à l'accueil");
        logoutButton.setOnAction(e -> router.logoutToMain());

        VBox root = new VBox(16, title, info, permissions, nextStep, logoutButton);
        root.setPadding(new Insets(20));
        return root;
    }
}
