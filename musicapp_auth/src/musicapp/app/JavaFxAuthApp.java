package musicapp.app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import musicapp.controller.AuthService;
import musicapp.model.Session;
import musicapp.persistence.UtilisateurRepository;
import musicapp.view.javafx.AuthRouter;

import java.io.IOException;
import java.nio.file.Path;

public class JavaFxAuthApp extends Application {
    private AuthService authService;

    @Override
    public void start(Stage stage) {
        UtilisateurRepository repository = new UtilisateurRepository(Path.of("data", "utilisateurs.ser"));
        authService = new AuthService(repository, new Session());

        try {
            authService.chargerDonnees();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Chargement initial impossible: " + e.getMessage());
        }

        AuthRouter router = new AuthRouter(stage, authService);
        Scene scene = router.buildMainScene();

        stage.setTitle("MusicApp - Authentification");
        stage.setMinWidth(860);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.show();

        stage.setOnCloseRequest(event -> {
            try {
                authService.sauvegarderDonnees();
            } catch (IOException e) {
                System.err.println("Sauvegarde impossible: " + e.getMessage());
            }
            Platform.exit();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
