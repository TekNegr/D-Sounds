package dsounds.repositories;

import dsounds.models.Review;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class ReviewRepository {

    private static final Path ROOT_STORAGE = Path.of("local_storage");
    private static final Path REVIEWS_DIR = ROOT_STORAGE.resolve("reviews");

    private ReviewRepository() {
    }

    public static List<Review> loadAllReviews() throws IOException {
        List<Review> reviews = new ArrayList<>();

        if (!Files.exists(REVIEWS_DIR)) {
            return reviews;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(REVIEWS_DIR, "*.properties")) {
            for (Path reviewFile : stream) {
                Review review = loadReview(reviewFile);
                if (review != null) {
                    reviews.add(review);
                }
            }
        }

        return reviews;
    }

    public static List<Review> loadReviewsForSong(String songId) throws IOException {
        List<Review> reviews = new ArrayList<>();
        for (Review review : loadAllReviews()) {
            if (songId != null && songId.equals(review.getSongId())) {
                reviews.add(review);
            }
        }
        return reviews;
    }

    public static Review findReview(String songId, String userId) throws IOException {
        if (songId == null || userId == null) {
            return null;
        }
        Path reviewPath = reviewPath(songId, userId);
        if (!Files.exists(reviewPath)) {
            return null;
        }
        return loadReview(reviewPath);
    }

    public static void saveReview(Review review) throws IOException {
        Files.createDirectories(REVIEWS_DIR);

        Properties metadata = new Properties();
        metadata.setProperty("id", safe(review.getId()));
        metadata.setProperty("songId", safe(review.getSongId()));
        metadata.setProperty("userId", safe(review.getUserId()));
        metadata.setProperty("liked", Boolean.toString(review.isLiked()));
        metadata.setProperty("comment", safe(review.getComment()));
        metadata.setProperty("createdAt", review.getCreatedAt() != null ? review.getCreatedAt().toString() : Instant.now().toString());
        metadata.setProperty("updatedAt", review.getUpdatedAt() != null ? review.getUpdatedAt().toString() : Instant.now().toString());

        Path reviewPath = reviewPath(review.getSongId(), review.getUserId());
        try (OutputStream output = Files.newOutputStream(reviewPath)) {
            metadata.store(output, "Review metadata");
        }
    }

    public static void deleteReview(String songId, String userId) throws IOException {
        Files.deleteIfExists(reviewPath(songId, userId));
    }

    public static int countLikesForSong(String songId) throws IOException {
        int count = 0;
        for (Review review : loadReviewsForSong(songId)) {
            if (review.isLiked()) {
                count++;
            }
        }
        return count;
    }

    public static int countDislikesForSong(String songId) throws IOException {
        int count = 0;
        for (Review review : loadReviewsForSong(songId)) {
            if (!review.isLiked()) {
                count++;
            }
        }
        return count;
    }

    private static Review loadReview(Path reviewFile) {
        Properties metadata = new Properties();
        try (InputStream input = Files.newInputStream(reviewFile)) {
            metadata.load(input);

            Review review = new Review();
            review.setId(metadata.getProperty("id", ""));
            review.setSongId(metadata.getProperty("songId", ""));
            review.setUserId(metadata.getProperty("userId", ""));
            review.setLiked(Boolean.parseBoolean(metadata.getProperty("liked", "false")));
            review.setComment(metadata.getProperty("comment", ""));

            String createdAt = metadata.getProperty("createdAt");
            if (createdAt != null && !createdAt.isBlank()) {
                try {
                    review.setCreatedAt(Instant.parse(createdAt));
                } catch (RuntimeException ex) {
                    review.setCreatedAt(Instant.now());
                }
            }

            String updatedAt = metadata.getProperty("updatedAt");
            if (updatedAt != null && !updatedAt.isBlank()) {
                try {
                    review.setUpdatedAt(Instant.parse(updatedAt));
                } catch (RuntimeException ex) {
                    review.setUpdatedAt(review.getCreatedAt());
                }
            }

            return review;
        } catch (IOException ex) {
            return null;
        }
    }

    private static Path reviewPath(String songId, String userId) {
        String safeSongId = safeFilePart(songId);
        String safeUserId = safeFilePart(userId);
        return REVIEWS_DIR.resolve(safeSongId + "__" + safeUserId + ".properties");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String safeFilePart(String value) {
        return value == null ? "unknown" : value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
