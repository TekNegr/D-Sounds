package dsounds.models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Playlist model for grouping songs together.
 */
public class Playlist {

    public enum Visibility {
        PUBLIC,
        PRIVATE
    }

    private String id;
    private String userId;
    private String ownerUsername;
    private String name;
    private String description;
    private List<String> songIds;
    private Visibility visibility;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Collaborateurs de la playlist (format persisté : "user1:EDITOR,user2:VIEWER").
     * Vide pour les playlists non collaboratives.
     * Ajouté par Laksman — persistance des collaborateurs.
     */
    private String collaborators;

    public Playlist() {
        this.id = UUID.randomUUID().toString();
        this.songIds = new ArrayList<>();
        this.visibility = Visibility.PUBLIC;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public Playlist(String ownerUsername, String name) {
        this();
        this.ownerUsername = ownerUsername;
        this.name = name;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
        this.updatedAt = Instant.now();
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
        this.updatedAt = Instant.now();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.updatedAt = Instant.now();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = Instant.now();
    }

    public List<String> getSongIds() {
        return songIds;
    }

    public void setSongIds(List<String> songIds) {
        this.songIds = songIds != null ? songIds : new ArrayList<>();
        this.updatedAt = Instant.now();
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility != null ? visibility : Visibility.PUBLIC;
        this.updatedAt = Instant.now();
    }

    public boolean isPublic() {
        return visibility == Visibility.PUBLIC;
    }

    public void addSong(String songId) {
        if (!songIds.contains(songId)) {
            songIds.add(songId);
            this.updatedAt = Instant.now();
        }
    }

    public void removeSong(String songId) {
        if (songIds.remove(songId)) {
            this.updatedAt = Instant.now();
        }
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

    /**
     * Retourne la chaîne de collaborateurs persistée (format "user1:EDITOR,user2:VIEWER").
     * @return la chaîne ou null si pas de collaborateurs
     */
    public String getCollaborators() {
        return collaborators;
    }

    /**
     * Définit la chaîne de collaborateurs (format "user1:EDITOR,user2:VIEWER").
     * @param collaborators la chaîne de collaborateurs
     */
    public void setCollaborators(String collaborators) {
        this.collaborators = collaborators;
        this.updatedAt = Instant.now();
    }

    @Override
    public String toString() {
        String visibilityLabel = visibility == Visibility.PUBLIC ? "Public" : "Private";
        return name + " [" + visibilityLabel + "] (" + songIds.size() + " songs)";
    }
}
