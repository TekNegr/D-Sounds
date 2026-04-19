package models;

import java.io.Serializable;
import java.util.Objects;

/**
 * Représente un morceau de musique dans le catalogue.
 */
public class Song implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String titre;
    private String artiste;
    private String album;
    private String genre;
    private int dureeSecondes;
    private int annee;
    private int nombreEcoutes;

    public Song(String id, String titre, String artiste, String album, String genre, int dureeSecondes, int annee) {
        this.id = id;
        this.titre = titre;
        this.artiste = artiste;
        this.album = album;
        this.genre = genre;
        this.dureeSecondes = dureeSecondes;
        this.annee = annee;
        this.nombreEcoutes = 0;
    }

    // ---- Getters / Setters ----
    public String getId() { return id; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getArtiste() { return artiste; }
    public void setArtiste(String artiste) { this.artiste = artiste; }
    public String getAlbum() { return album; }
    public void setAlbum(String album) { this.album = album; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public int getDureeSecondes() { return dureeSecondes; }
    public void setDureeSecondes(int dureeSecondes) { this.dureeSecondes = dureeSecondes; }
    public int getAnnee() { return annee; }
    public void setAnnee(int annee) { this.annee = annee; }
    public int getNombreEcoutes() { return nombreEcoutes; }

    /** Incrémente le compteur d'écoutes */
    public void incrementerEcoutes() { this.nombreEcoutes++; }

    /** Retourne la durée formatée en mm:ss */
    public String getDureeFormatee() {
        int min = dureeSecondes / 60;
        int sec = dureeSecondes % 60;
        return String.format("%d:%02d", min, sec);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Song)) return false;
        return Objects.equals(id, ((Song) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return titre + " - " + artiste + " (" + getDureeFormatee() + ")";
    }
}
