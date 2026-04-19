package models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Représente un utilisateur du système D-Sounds.
 * Peut être un Visiteur, un Abonné ou un Administrateur.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Rôles possibles */
    public enum Role { VISITEUR, ABONNE, ADMINISTRATEUR }

    private String id;
    private String pseudo;
    private String email;
    private String motDePasse;
    private Role role;
    private boolean suspendu;
    private List<String> playlistIds;
    private List<String> collabPlaylistIds;
    private List<String> historiqueEcoute;
    private int ecoutesSession;

    public User(String id, String pseudo, String email, String motDePasse, Role role) {
        this.id = id;
        this.pseudo = pseudo;
        this.email = email;
        this.motDePasse = motDePasse;
        this.role = role;
        this.suspendu = false;
        this.playlistIds = new ArrayList<>();
        this.collabPlaylistIds = new ArrayList<>();
        this.historiqueEcoute = new ArrayList<>();
        this.ecoutesSession = 0;
    }

    // ---- Getters / Setters ----
    public String getId() { return id; }
    public String getPseudo() { return pseudo; }
    public void setPseudo(String pseudo) { this.pseudo = pseudo; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isSuspendu() { return suspendu; }
    public void setSuspendu(boolean suspendu) { this.suspendu = suspendu; }
    public List<String> getPlaylistIds() { return playlistIds; }
    public List<String> getCollabPlaylistIds() { return collabPlaylistIds; }
    public List<String> getHistoriqueEcoute() { return historiqueEcoute; }
    public int getEcoutesSession() { return ecoutesSession; }
    public void incrementerEcoutesSession() { this.ecoutesSession++; }
    public void resetEcoutesSession() { this.ecoutesSession = 0; }

    // ---- Méthodes métier ----

    /** Un visiteur est limité à 5 écoutes par session */
    public boolean peutEcouter() {
        if (role == Role.VISITEUR) return ecoutesSession < 5;
        return role == Role.ABONNE || role == Role.ADMINISTRATEUR;
    }

    public void ajouterEcoute(String songId) {
        historiqueEcoute.add(songId);
        ecoutesSession++;
    }

    public void ajouterPlaylist(String playlistId) {
        if (!playlistIds.contains(playlistId)) playlistIds.add(playlistId);
    }

    public void retirerPlaylist(String playlistId) {
        playlistIds.remove(playlistId);
    }

    public void ajouterCollabPlaylist(String playlistId) {
        if (!collabPlaylistIds.contains(playlistId)) collabPlaylistIds.add(playlistId);
    }

    public void retirerCollabPlaylist(String playlistId) {
        collabPlaylistIds.remove(playlistId);
    }

    public boolean verifierMotDePasse(String mdp) {
        return this.motDePasse.equals(mdp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        return Objects.equals(id, ((User) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "User{id='" + id + "', pseudo='" + pseudo + "', role=" + role + "}";
    }
}
