package models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Représente une playlist de morceaux appartenant à un abonné.
 */
public class Playlist implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String nom;
    private String proprietaireId;  // ID du User propriétaire
    private List<String> songIds;   // IDs des morceaux dans la playlist
    private String dateCreation;

    public Playlist(String id, String nom, String proprietaireId) {
        this.id = id;
        this.nom = nom;
        this.proprietaireId = proprietaireId;
        this.songIds = new ArrayList<>();
        this.dateCreation = java.time.LocalDate.now().toString();
    }

    // ---- Getters / Setters ----
    public String getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getProprietaireId() { return proprietaireId; }
    public List<String> getSongIds() { return songIds; }
    public String getDateCreation() { return dateCreation; }

    // ---- Méthodes métier ----

    /**
     * Ajoute un morceau à la playlist.
     * @param songId l'ID du morceau à ajouter
     * @return true si ajouté, false si déjà présent
     */
    public boolean ajouterMorceau(String songId) {
        if (songIds.contains(songId)) return false;
        songIds.add(songId);
        return true;
    }

    /**
     * Retire un morceau de la playlist.
     * @param songId l'ID du morceau à retirer
     * @return true si retiré, false si non trouvé
     */
    public boolean retirerMorceau(String songId) {
        return songIds.remove(songId);
    }

    /**
     * Vérifie si un morceau est dans la playlist.
     * @param songId l'ID du morceau
     * @return true si le morceau est dans la playlist
     */
    public boolean contientMorceau(String songId) {
        return songIds.contains(songId);
    }

    /** Retourne le nombre de morceaux dans la playlist */
    public int getNombreMorceaux() {
        return songIds.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Playlist)) return false;
        return Objects.equals(id, ((Playlist) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return nom + " (" + songIds.size() + " morceaux)";
    }
}
