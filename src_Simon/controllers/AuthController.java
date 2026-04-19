package controllers;

import models.AuthSession;
import models.User;
import services.persistence.UserRepository;

/**
 * Contrôleur pour l'authentification et la gestion des comptes.
 */
public class AuthController {

    private UserRepository userRepo;
    private AuthSession session;

    public AuthController(UserRepository userRepo, AuthSession session) {
        this.userRepo = userRepo;
        this.session = session;
    }

    /**
     * Connecte un utilisateur avec email et mot de passe.
     * @param email l'email de l'utilisateur
     * @param motDePasse le mot de passe
     * @return l'utilisateur connecté
     * @throws IllegalArgumentException si les identifiants sont incorrects
     * @throws IllegalStateException si le compte est suspendu
     */
    public User connecter(String email, String motDePasse) {
        User user = userRepo.trouverParEmail(email);
        if (user == null || !user.verifierMotDePasse(motDePasse)) {
            throw new IllegalArgumentException("Email ou mot de passe incorrect.");
        }
        if (user.isSuspendu()) {
            throw new IllegalStateException("Ce compte est suspendu.");
        }
        session.connecter(user);
        return user;
    }

    /** Déconnecte l'utilisateur courant */
    public void deconnecter() {
        session.deconnecter();
    }

    /**
     * Crée un nouveau compte abonné.
     * @param pseudo le nom d'utilisateur
     * @param email l'email
     * @param motDePasse le mot de passe
     * @return l'utilisateur créé
     * @throws IllegalArgumentException si l'email est déjà utilisé
     */
    public User creerCompte(String pseudo, String email, String motDePasse) {
        if (userRepo.trouverParEmail(email) != null) {
            throw new IllegalArgumentException("Un compte avec cet email existe déjà.");
        }
        String id = "USR-" + System.currentTimeMillis();
        User user = new User(id, pseudo, email, motDePasse, User.Role.ABONNE);
        userRepo.sauvegarder(user);
        return user;
    }

    /**
     * Connecte en tant que visiteur (sans compte).
     * @return un utilisateur visiteur temporaire
     */
    public User continuerEnVisiteur() {
        String id = "VISIT-" + System.currentTimeMillis();
        User visiteur = new User(id, "Visiteur", "", "", User.Role.VISITEUR);
        session.connecter(visiteur);
        return visiteur;
    }

    public AuthSession getSession() { return session; }
    public User getUtilisateurConnecte() { return session.getUtilisateurConnecte(); }
}
