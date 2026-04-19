package dsounds.controllers;

import dsounds.models.Review;
import dsounds.repositories.ReviewRepository;

import java.io.IOException;
import java.util.List;

public class ReviewController {

    private static final int MAX_COMMENT_LENGTH = 140;

    public Review upsertReview(String songId, String userId, boolean liked, String comment) throws IOException {
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

    public void deleteCurrentUserReview(String songId, String userId) throws IOException {
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
