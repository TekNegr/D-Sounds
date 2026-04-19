package models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Représente un avis / une note laissée par un abonné sur un morceau.
 * Un abonné ne peut laisser qu'un seul avis par morceau.
 */
public class Review implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String userId;       // ID de l'abonné qui a laissé l'avis
    private String songId;       // ID du morceau noté
    private int note;            // Note entre 1 et 5
    private String commentaire;  // Commentaire optionnel
    private String dateCreation;
    private String dateModification;

    /**
     * Crée un avis.
     * @param id identifiant unique de l'avis
     * @param userId ID de l'abonné
     * @param songId ID du morceau
     * @param note note entre 1 et 5
     * @param commentaire commentaire (peut être null ou vide)
     * @throws IllegalArgumentException si la note n'est pas entre 1 et 5
     */
    public Review(String id, String userId, String songId, int note, String commentaire) {
        if (note < 1 || note > 5) {
            throw new IllegalArgumentException("La note doit être entre 1 et 5. Reçu : " + note);
        }
        this.id = id;
        this.userId = userId;
        this.songId = songId;
        this.note = note;
        this.commentaire = commentaire != null ? commentaire : "";
        this.dateCreation = LocalDateTime.now().toString();
        this.dateModification = this.dateCreation;
    }

    // ---- Getters ----
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getSongId() { return songId; }
    public int getNote() { return note; }
    public String getCommentaire() { return commentaire; }
    public String getDateCreation() { return dateCreation; }
    public String getDateModification() { return dateModification; }

    // ---- Modification ----

    /**
     * Modifie la note de l'avis.
     * @param note nouvelle note entre 1 et 5
     * @throws IllegalArgumentException si la note n'est pas entre 1 et 5
     */
    public void setNote(int note) {
        if (note < 1 || note > 5) {
            throw new IllegalArgumentException("La note doit être entre 1 et 5. Reçu : " + note);
        }
        this.note = note;
        this.dateModification = LocalDateTime.now().toString();
    }

    /**
     * Modifie le commentaire de l'avis.
     * @param commentaire nouveau commentaire
     */
    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire != null ? commentaire : "";
        this.dateModification = LocalDateTime.now().toString();
    }

    /**
     * Retourne la note sous forme d'étoiles (pour affichage).
     * @return une chaîne de caractères avec des étoiles
     */
    public String getNoteEtoiles() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < note; i++) sb.append("★");
        for (int i = note; i < 5; i++) sb.append("☆");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Review)) return false;
        return Objects.equals(id, ((Review) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return getNoteEtoiles() + " - " + (commentaire.isEmpty() ? "(pas de commentaire)" : commentaire);
    }
}
