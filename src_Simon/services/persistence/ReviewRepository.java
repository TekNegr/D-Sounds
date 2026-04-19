package services.persistence;

import models.Review;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository pour la persistance des avis/reviews.
 * Utilise la sérialisation Java.
 */
public class ReviewRepository {

    private static final String FICHIER = "data/reviews.dat";
    private Map<String, Review> reviews;

    public ReviewRepository() {
        this.reviews = new HashMap<>();
        charger();
    }

    public void sauvegarder(Review review) {
        reviews.put(review.getId(), review);
        persister();
    }

    public Review trouverParId(String id) {
        return reviews.get(id);
    }

    /**
     * Trouve l'avis d'un utilisateur sur un morceau donné.
     * @param userId l'ID de l'utilisateur
     * @param songId l'ID du morceau
     * @return l'avis, ou null si pas trouvé
     */
    public Review trouverParUserEtSong(String userId, String songId) {
        for (Review r : reviews.values()) {
            if (r.getUserId().equals(userId) && r.getSongId().equals(songId)) {
                return r;
            }
        }
        return null;
    }

    /**
     * Trouve tous les avis sur un morceau.
     * @param songId l'ID du morceau
     * @return la liste des avis
     */
    public List<Review> trouverParSong(String songId) {
        List<Review> result = new ArrayList<>();
        for (Review r : reviews.values()) {
            if (r.getSongId().equals(songId)) result.add(r);
        }
        return result;
    }

    /**
     * Trouve tous les avis d'un utilisateur.
     * @param userId l'ID de l'utilisateur
     * @return la liste des avis
     */
    public List<Review> trouverParUser(String userId) {
        List<Review> result = new ArrayList<>();
        for (Review r : reviews.values()) {
            if (r.getUserId().equals(userId)) result.add(r);
        }
        return result;
    }

    public void supprimer(String id) {
        reviews.remove(id);
        persister();
    }

    public List<Review> trouverTout() {
        return new ArrayList<>(reviews.values());
    }

    private void persister() {
        try {
            new File("data").mkdirs();
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FICHIER));
            oos.writeObject(reviews);
            oos.close();
        } catch (IOException e) {
            System.err.println("Erreur sauvegarde reviews : " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void charger() {
        File f = new File(FICHIER);
        if (!f.exists()) return;
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
            reviews = (Map<String, Review>) ois.readObject();
            ois.close();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erreur chargement reviews : " + e.getMessage());
            reviews = new HashMap<>();
        }
    }
}
