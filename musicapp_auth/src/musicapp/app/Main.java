package musicapp.app;

import musicapp.controller.AuthService;
import musicapp.model.Session;
import musicapp.persistence.UtilisateurRepository;
import musicapp.view.ConsoleAuthView;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        UtilisateurRepository repository = new UtilisateurRepository(Path.of("data", "utilisateurs.ser"));
        Session session = new Session();
        AuthService authService = new AuthService(repository, session);
        ConsoleAuthView view = new ConsoleAuthView(authService);
        view.lancer();
    }
}
