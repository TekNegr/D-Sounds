package dsounds.models;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Review model for song reviews and ratings.
 */
public class Review implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String songId;
    private String userId;
    private boolean liked;
    private String comment;
    private Instant createdAt;
    private Instant updatedAt;

    public Review() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public Review(String songId, String userId, boolean liked, String comment) {
        this();
        this.songId = songId;
        this.userId = userId;
        this.liked = liked;
        this.comment = comment;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSongId() {
        return songId;
    }

    public void setSongId(String songId) {
        this.songId = songId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isLiked() {
        return liked;
    }

    public void setLiked(boolean liked) {
        this.liked = liked;
        this.updatedAt = Instant.now();
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return liked ? "Like" : "Dislike";
    }
}
