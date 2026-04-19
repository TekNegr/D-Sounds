package services.persistence;

import models.Song;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Repository pour la persistance des morceaux de musique.
 * Utilise la sérialisation Java.
 */
public class SongRepository {

    private static final String FICHIER = "data/songs.dat";
    private Map<String, Song> songs;

    public SongRepository() {
        this.songs = new HashMap<>();
        charger();
    }

    public void sauvegarder(Song song) {
        songs.put(song.getId(), song);
        persister();
    }

    public Song trouverParId(String id) {
        return songs.get(id);
    }

    /**
     * Recherche de morceaux par titre (recherche partielle, insensible à la casse).
     * @param titre le terme de recherche
     * @return la liste des morceaux correspondants
     */
    public List<Song> rechercherParTitre(String titre) {
        String recherche = titre.toLowerCase();
        List<Song> result = new ArrayList<>();
        for (Song s : songs.values()) {
            if (s.getTitre().toLowerCase().contains(recherche)) result.add(s);
        }
        return result;
    }

    /**
     * Recherche de morceaux par artiste.
     * @param artiste le nom de l'artiste
     * @return la liste des morceaux
     */
    public List<Song> rechercherParArtiste(String artiste) {
        String recherche = artiste.toLowerCase();
        List<Song> result = new ArrayList<>();
        for (Song s : songs.values()) {
            if (s.getArtiste().toLowerCase().contains(recherche)) result.add(s);
        }
        return result;
    }

    /**
     * Recherche de morceaux par genre.
     * @param genre le genre musical
     * @return la liste des morceaux
     */
    public List<Song> rechercherParGenre(String genre) {
        String recherche = genre.toLowerCase();
        List<Song> result = new ArrayList<>();
        for (Song s : songs.values()) {
            if (s.getGenre().toLowerCase().contains(recherche)) result.add(s);
        }
        return result;
    }

    public void supprimer(String id) {
        songs.remove(id);
        persister();
    }

    public List<Song> trouverTout() {
        return new ArrayList<>(songs.values());
    }

    public int compter() {
        return songs.size();
    }

    /** Retourne le nombre total d'écoutes de tous les morceaux */
    public int getTotalEcoutes() {
        int total = 0;
        for (Song s : songs.values()) {
            total += s.getNombreEcoutes();
        }
        return total;
    }

    private void persister() {
        try {
            new File("data").mkdirs();
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FICHIER));
            oos.writeObject(songs);
            oos.close();
        } catch (IOException e) {
            System.err.println("Erreur sauvegarde songs : " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void charger() {
        File f = new File(FICHIER);
        if (!f.exists()) return;
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
            songs = (Map<String, Song>) ois.readObject();
            ois.close();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erreur chargement songs : " + e.getMessage());
            songs = new HashMap<>();
        }
    }
}
