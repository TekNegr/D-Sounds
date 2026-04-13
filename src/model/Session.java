package model;

public class Session {
    private Utilisateur utilisateurCourant;

    public boolean estConnecte() {
        return utilisateurCourant != null;
    }

    public Utilisateur getUtilisateurCourant() {
        return utilisateurCourant;
    }

    public void ouvrir(Utilisateur utilisateur) {
        this.utilisateurCourant = utilisateur;
    }

    public void fermer() {
        this.utilisateurCourant = null;
    }

    public boolean aRole(Role role) {
        return estConnecte() && utilisateurCourant.getRole() == role;
    }
}
