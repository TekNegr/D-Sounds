package controllers;

import models.Review;
import models.Song;
import models.User;
import services.persistence.ReviewRepository;
import services.persistence.SongRepository;
import services.persistence.UserRepository;

import java.util.List;

/**
 * Contrôleur pour le système de notation et d'avis sur les morceaux.
 * Gère la création, modification, suppression et consultation des reviews.
 */
public class ReviewController {

    private ReviewRepository reviewRepo;
    private UserRepository userRepo;
    private SongRepository songRepo;

    public ReviewController(ReviewRepository reviewRepo, UserRepository userRepo, SongRepository songRepo) {
        this.reviewRepo = reviewRepo;
        this.userRepo = userRepo;
        this.songRepo = songRepo;
    }

    /**
     * Crée ou met à jour un avis d'un abonné sur un morceau.
     * Un abonné ne peut laisser qu'un seul avis par morceau.
     * Si un avis existe déjà, il est mis à jour.
     * @param userId l'ID de l'abonné
     * @param songId l'ID du morceau
     * @param note la note entre 1 et 5
     * @param commentaire le commentaire (peut être null)
     * @return l'avis créé ou mis à jour
     */
    public Review laisserAvis(String userId, String songId, int note, String commentaire) {
        User user = userRepo.trouverParId(userId);
        if (user == null || user.getRole() != User.Role.ABONNE) {
            throw new IllegalStateException("Seuls les abonnés peuvent laisser un avis.");
        }
        Song song = songRepo.trouverParId(songId);
        if (song == null) {
            throw new IllegalArgumentException("Morceau introuvable : " + songId);
        }

        // Vérifier si un avis existe déjà pour ce user/song
        Review existant = reviewRepo.trouverParUserEtSong(userId, songId);
        if (existant != null) {
            existant.setNote(note);
            existant.setCommentaire(commentaire);
            reviewRepo.sauvegarder(existant);
            return existant;
        }

        // Créer un nouvel avis
        String id = "REV-" + System.currentTimeMillis();
        Review review = new Review(id, userId, songId, note, commentaire);
        reviewRepo.sauvegarder(review);
        return review;
    }

    /**
     * Modifie la note d'un avis existant.
     * @param reviewId l'ID de l'avis
     * @param userId l'ID de l'utilisateur (doit être l'auteur)
     * @param nouvelleNote la nouvelle note
     */
    public void modifierNote(String reviewId, String userId, int nouvelleNote) {
        Review review = reviewRepo.trouverParId(reviewId);
        if (review == null) throw new IllegalArgumentException("Avis introuvable.");
        if (!review.getUserId().equals(userId)) {
            throw new SecurityException("Vous ne pouvez modifier que vos propres avis.");
        }
        review.setNote(nouvelleNote);
        reviewRepo.sauvegarder(review);
    }

    /**
     * Modifie le commentaire d'un avis existant.
     * @param reviewId l'ID de l'avis
     * @param userId l'ID de l'utilisateur (doit être l'auteur)
     * @param nouveauCommentaire le nouveau commentaire
     */
    public void modifierCommentaire(String reviewId, String userId, String nouveauCommentaire) {
        Review review = reviewRepo.trouverParId(reviewId);
        if (review == null) throw new IllegalArgumentException("Avis introuvable.");
        if (!review.getUserId().equals(userId)) {
            throw new SecurityException("Vous ne pouvez modifier que vos propres avis.");
        }
        review.setCommentaire(nouveauCommentaire);
        reviewRepo.sauvegarder(review);
    }

    /**
     * Supprime un avis.
     * @param reviewId l'ID de l'avis
     * @param userId l'ID de l'utilisateur (doit être l'auteur)
     */
    public void supprimerAvis(String reviewId, String userId) {
        Review review = reviewRepo.trouverParId(reviewId);
        if (review == null) throw new IllegalArgumentException("Avis introuvable.");
        if (!review.getUserId().equals(userId)) {
            throw new SecurityException("Vous ne pouvez supprimer que vos propres avis.");
        }
        reviewRepo.supprimer(reviewId);
    }

    /**
     * Récupère tous les avis sur un morceau.
     * @param songId l'ID du morceau
     * @return la liste des avis
     */
    public List<Review> getAvisParMorceau(String songId) {
        return reviewRepo.trouverParSong(songId);
    }

    /**
     * Récupère tous les avis d'un utilisateur.
     * @param userId l'ID de l'utilisateur
     * @return la liste des avis
     */
    public List<Review> getAvisParUtilisateur(String userId) {
        return reviewRepo.trouverParUser(userId);
    }

    /**
     * Calcule la note moyenne d'un morceau.
     * @param songId l'ID du morceau
     * @return la note moyenne (0.0 si aucun avis)
     */
    public double getNoteMoyenne(String songId) {
        List<Review> avis = reviewRepo.trouverParSong(songId);
        if (avis.isEmpty()) return 0.0;
        double somme = 0;
        for (Review r : avis) {
            somme += r.getNote();
        }
        return somme / avis.size();
    }

    /**
     * Retourne le nombre d'avis sur un morceau.
     * @param songId l'ID du morceau
     * @return le nombre d'avis
     */
    public int getNombreAvis(String songId) {
        return reviewRepo.trouverParSong(songId).size();
    }
}
