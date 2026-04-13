package musicapp.view;

import musicapp.controller.AuthException;
import musicapp.controller.AuthService;
import musicapp.model.CataloguePermissions;
import musicapp.model.Role;
import musicapp.model.Utilisateur;

import java.io.IOException;
import java.util.Scanner;

public class ConsoleAuthView {
    private final AuthService authService;
    private final Scanner scanner;

    public ConsoleAuthView(AuthService authService) {
        this.authService = authService;
        this.scanner = new Scanner(System.in);
    }

    public void lancer() {
        try {
            authService.chargerDonnees();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Avertissement : impossible de charger les utilisateurs : " + e.getMessage());
        }

        boolean quitter = false;
        while (!quitter) {
            afficherMenuPrincipal();
            String choix = scanner.nextLine().trim();
            switch (choix) {
                case "1" -> traiterConnexion(Role.ADMIN);
                case "2" -> traiterConnexion(Role.ABONNE);
                case "3" -> traiterCreationCompte();
                case "4" -> traiterModeVisiteur();
                case "5" -> quitter = true;
                case "6" -> afficherUtilisateurs();
                default -> System.out.println("Choix invalide.");
            }
        }

        try {
            authService.sauvegarderDonnees();
            System.out.println("Données sauvegardées.");
        } catch (IOException e) {
            System.out.println("Erreur de sauvegarde : " + e.getMessage());
        }
    }

    private void afficherMenuPrincipal() {
        System.out.println("\n=== MUSICAPP ===");
        System.out.println("1. Se connecter en tant qu'administrateur");
        System.out.println("2. Se connecter en tant qu'abonné");
        System.out.println("3. Créer un compte abonné");
        System.out.println("4. Continuer en tant que visiteur");
        System.out.println("5. Quitter");
        System.out.println("6. [debug] Lister les utilisateurs");
        System.out.print("Votre choix : ");
    }

    private void traiterConnexion(Role roleAttendu) {
        try {
            Utilisateur utilisateur = demanderConnexion(roleAttendu);
            afficherSession(utilisateur);
            if (utilisateur.getRole() == Role.ADMIN) {
                menuAdmin();
            } else {
                attendreRetourAccueil();
            }
        } catch (AuthException e) {
            System.out.println("Connexion refusée : " + e.getMessage());
        } finally {
            authService.deconnecter();
        }
    }

    private Utilisateur demanderConnexion(Role roleAttendu) throws AuthException {
        System.out.print("Nom d'utilisateur : ");
        String nomUtilisateur = scanner.nextLine();
        System.out.print("Mot de passe : ");
        String motDePasse = scanner.nextLine();
        return authService.connecter(nomUtilisateur, motDePasse, roleAttendu);
    }

    private void traiterCreationCompte() {
        try {
            System.out.print("Choisissez un nom d'utilisateur : ");
            String nomUtilisateur = scanner.nextLine();
            System.out.print("Choisissez un mot de passe : ");
            String motDePasse = scanner.nextLine();
            System.out.print("Confirmez le mot de passe : ");
            String confirmation = scanner.nextLine();

            if (!motDePasse.equals(confirmation)) {
                throw new AuthException("Les mots de passe ne correspondent pas.");
            }

            authService.inscrireAbonne(nomUtilisateur, motDePasse);
            System.out.println("Compte créé avec succès.");
        } catch (AuthException e) {
            System.out.println("Création impossible : " + e.getMessage());
        }
    }

    private void traiterModeVisiteur() {
        authService.continuerCommeVisiteur();
        afficherSession(authService.getSession().getUtilisateurCourant());
        attendreRetourAccueil();
        authService.deconnecter();
    }

    private void afficherSession(Utilisateur utilisateur) {
        System.out.println("Connecté en tant que : " + utilisateur.getNomUtilisateur() + " [" + utilisateur.getRole() + "]");
        System.out.println("Peut consulter le catalogue : " + CataloguePermissions.peutConsulterCatalogue(authService.getSession()));
        System.out.println("Peut créer des playlists : " + CataloguePermissions.peutCreerPlaylist(authService.getSession()));
        System.out.println("Peut gérer le catalogue : " + CataloguePermissions.peutGererCatalogue(authService.getSession()));
    }

    private void menuAdmin() {
        boolean retour = false;
        while (!retour) {
            System.out.println("\n--- Menu administrateur ---");
            System.out.println("1. Lister les utilisateurs");
            System.out.println("2. Suspendre un compte");
            System.out.println("3. Réactiver un compte");
            System.out.println("4. Supprimer un compte");
            System.out.println("5. Retour accueil");
            System.out.print("Votre choix : ");
            String choix = scanner.nextLine().trim();
            try {
                switch (choix) {
                    case "1" -> afficherUtilisateurs();
                    case "2" -> changerEtatCompte(true);
                    case "3" -> changerEtatCompte(false);
                    case "4" -> supprimerCompte();
                    case "5" -> retour = true;
                    default -> System.out.println("Choix invalide.");
                }
            } catch (AuthException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void changerEtatCompte(boolean suspendre) throws AuthException {
        System.out.print("Nom d'utilisateur ciblé : ");
        String nomUtilisateur = scanner.nextLine();
        if (suspendre) {
            authService.suspendreCompte(nomUtilisateur);
            System.out.println("Compte suspendu.");
        } else {
            authService.reactiverCompte(nomUtilisateur);
            System.out.println("Compte réactivé.");
        }
    }

    private void supprimerCompte() throws AuthException {
        System.out.print("Nom d'utilisateur à supprimer : ");
        String nomUtilisateur = scanner.nextLine();
        authService.supprimerCompte(nomUtilisateur);
        System.out.println("Compte supprimé.");
    }

    private void attendreRetourAccueil() {
        System.out.println("Appuie sur Entrée pour revenir à l'accueil...");
        scanner.nextLine();
    }

    private void afficherUtilisateurs() {
        System.out.println("\n--- Utilisateurs enregistrés ---");
        for (Utilisateur utilisateur : authService.listerUtilisateurs()) {
            System.out.printf("- %s [%s] actif=%s%n",
                    utilisateur.getNomUtilisateur(),
                    utilisateur.getRole(),
                    utilisateur.isActif());
        }
    }
}
