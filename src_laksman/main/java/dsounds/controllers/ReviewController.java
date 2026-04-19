package dsounds.controllers;

import dsounds.App;
import dsounds.models.Review;
import dsounds.repositories.ReviewRepository;
import dsounds.security.RoleGuard;

import java.io.IOException;
import java.util.List;

/**
 * ReviewController — gestion des avis (notes et commentaires) sur les morceaux.
 *
 * <p>Les avis sont réservés aux abonnés. Les visiteurs peuvent consulter
 * les statistiques mais ne peuvent pas soumettre ni modifier d'avis.</p>
 *
 * <p><b>Modifié par Laksman</b> — restriction des avis aux abonnés via RoleGuard.</p>
 */
public class ReviewController {

    private static final int MAX_COMMENT_LENGTH = 140;

    /**
     * Soumet ou met à jour un avis sur un morceau.
     * Réservé aux abonnés — les visiteurs ne peuvent pas laisser d'avis.
     *
     * @throws AuthException si l'utilisateur n'est pas abonné ou admin
     */
    public Review upsertReview(String songId, String userId, boolean liked, String comment)
            throws IOException, AuthException {
        // Vérification de rôle : seuls les abonnés peuvent laisser un avis (Laksman).
        RoleGuard.requireNotVisitor(App.getAuthController().getSession());

        String normalizedComment = normalizeComment(comment);
        Review review = ReviewRepository.findReview(songId, userId);

        if (review == null) {
            review = new Review(songId, userId, liked, normalizedComment);
        } else {
            review.setLiked(liked);
            review.setComment(normalizedComment);
        }

        ReviewRepository.saveReview(review);
        return review;
    }

    public Review findCurrentUserReview(String songId, String userId) throws IOException {
        return ReviewRepository.findReview(songId, userId);
    }

    public ReviewStats getStatsForSong(String songId) throws IOException {
        List<Review> reviews = ReviewRepository.loadReviewsForSong(songId);
        int likes = 0;
        int dislikes = 0;

        for (Review review : reviews) {
            if (review.isLiked()) {
                likes++;
            } else {
                dislikes++;
            }
        }

        return new ReviewStats(likes, dislikes);
    }

    /**
     * Supprime l'avis de l'utilisateur courant sur un morceau.
     * Réservé aux abonnés.
     *
     * @throws AuthException si l'utilisateur n'est pas abonné ou admin
     */
    public void deleteCurrentUserReview(String songId, String userId)
            throws IOException, AuthException {
        // Un visiteur ne peut pas supprimer d'avis (il n'en a pas) (Laksman).
        RoleGuard.requireNotVisitor(App.getAuthController().getSession());
        ReviewRepository.deleteReview(songId, userId);
    }

    private static String normalizeComment(String comment) {
        if (comment == null) {
            return "";
        }
        String cleaned = comment.replace("\r", " ").replace("\n", " ").trim();
        if (cleaned.length() > MAX_COMMENT_LENGTH) {
            return cleaned.substring(0, MAX_COMMENT_LENGTH);
        }
        return cleaned;
    }

    public static final class ReviewStats {
        private final int likes;
        private final int dislikes;

        public ReviewStats(int likes, int dislikes) {
            this.likes = likes;
            this.dislikes = dislikes;
        }

        public int getLikes() {
            return likes;
        }

        public int getDislikes() {
            return dislikes;
        }
    }
}
