package musicapp.model;

import java.io.Serializable;
import java.util.Objects;

public abstract class Utilisateur implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private String nomUtilisateur;
    private String motDePasse;
    private boolean actif;

    protected Utilisateur(String id, String nomUtilisateur, String motDePasse) {
        this.id = Objects.requireNonNull(id, "id");
        setNomUtilisateur(nomUtilisateur);
        setMotDePasse(motDePasse);
        this.actif = true;
    }

    public String getId() {
        return id;
    }

    public String getNomUtilisateur() {
        return nomUtilisateur;
    }

    public void setNomUtilisateur(String nomUtilisateur) {
        if (nomUtilisateur == null || nomUtilisateur.isBlank()) {
            throw new IllegalArgumentException("Le nom d'utilisateur est obligatoire.");
        }
        this.nomUtilisateur = nomUtilisateur.trim();
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        if (motDePasse == null || motDePasse.length() < 4) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 4 caractères.");
        }
        this.motDePasse = motDePasse;
    }

    public boolean isActif() {
        return actif;
    }

    public void suspendre() {
        this.actif = false;
    }

    public void reactiver() {
        this.actif = true;
    }

    public boolean verifierMotDePasse(String tentative) {
        return motDePasse.equals(tentative);
    }

    public abstract Role getRole();

    @Override
    public String toString() {
        return "Utilisateur{" +
                "id='" + id + '\'' +
                ", nomUtilisateur='" + nomUtilisateur + '\'' +
                ", role=" + getRole() +
                ", actif=" + actif +
                '}';
    }
}
