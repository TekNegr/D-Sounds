package models;

import java.io.Serializable;

/**
 * Représente la session d'un utilisateur connecté.
 */
public class AuthSession implements Serializable {
    private static final long serialVersionUID = 1L;

    private User utilisateurConnecte;
    private boolean active;

    public AuthSession() {
        this.utilisateurConnecte = null;
        this.active = false;
    }

    /**
     * Connecte un utilisateur.
     * @param user l'utilisateur à connecter
     */
    public void connecter(User user) {
        this.utilisateurConnecte = user;
        this.active = true;
        user.resetEcoutesSession();
    }

    /** Déconnecte l'utilisateur courant */
    public void deconnecter() {
        this.utilisateurConnecte = null;
        this.active = false;
    }

    public User getUtilisateurConnecte() { return utilisateurConnecte; }
    public boolean isActive() { return active; }

    /** Vérifie si l'utilisateur connecté est abonné */
    public boolean estAbonne() {
        return active && utilisateurConnecte != null 
               && utilisateurConnecte.getRole() == User.Role.ABONNE;
    }

    /** Vérifie si l'utilisateur connecté est administrateur */
    public boolean estAdmin() {
        return active && utilisateurConnecte != null 
               && utilisateurConnecte.getRole() == User.Role.ADMINISTRATEUR;
    }
}
