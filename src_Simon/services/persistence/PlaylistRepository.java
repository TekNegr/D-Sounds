package services.persistence;

import models.Playlist;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository pour la persistance des playlists (standard et collaboratives).
 * Utilise la sérialisation Java pour sauvegarder/charger les données.
 */
public class PlaylistRepository {

    private static final String FICHIER = "data/playlists.dat";
    private Map<String, Playlist> playlists;

    public PlaylistRepository() {
        this.playlists = new HashMap<>();
        charger();
    }

    /**
     * Sauvegarde ou met à jour une playlist.
     * @param playlist la playlist à sauvegarder
     */
    public void sauvegarder(Playlist playlist) {
        playlists.put(playlist.getId(), playlist);
        persister();
    }

    /**
     * Trouve une playlist par son ID.
     * @param id l'identifiant de la playlist
     * @return la playlist trouvée, ou null
     */
    public Playlist trouverParId(String id) {
        return playlists.get(id);
    }

    /**
     * Trouve toutes les playlists d'un propriétaire.
     * @param userId l'ID du propriétaire
     * @return la liste des playlists
     */
    public List<Playlist> trouverParProprietaire(String userId) {
        List<Playlist> result = new ArrayList<>();
        for (Playlist pl : playlists.values()) {
            if (pl.getProprietaireId().equals(userId)) {
                result.add(pl);
            }
        }
        return result;
    }

    /**
     * Supprime une playlist.
     * @param id l'identifiant de la playlist
     */
    public void supprimer(String id) {
        playlists.remove(id);
        persister();
    }

    /** Retourne toutes les playlists */
    public List<Playlist> trouverTout() {
        return new ArrayList<>(playlists.values());
    }

    /** Persiste les données sur disque */
    private void persister() {
        try {
            new File("data").mkdirs();
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FICHIER));
            oos.writeObject(playlists);
            oos.close();
        } catch (IOException e) {
            System.err.println("Erreur sauvegarde playlists : " + e.getMessage());
        }
    }

    /** Charge les données depuis le disque */
    @SuppressWarnings("unchecked")
    private void charger() {
        File f = new File(FICHIER);
        if (!f.exists()) return;
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
            playlists = (Map<String, Playlist>) ois.readObject();
            ois.close();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erreur chargement playlists : " + e.getMessage());
            playlists = new HashMap<>();
        }
    }
}
