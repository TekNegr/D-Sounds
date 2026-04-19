package dsounds.models;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Album implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String description;
    private List<String> songIds;
    private Instant createdAt;
    private Instant updatedAt;

    public Album() {
        this.id = UUID.randomUUID().toString();
        this.songIds = new ArrayList<>();
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public Album(String name) {
        this();
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
        return Collections.unmodifiableList(songIds);
    }

    public void setSongIds(List<String> songIds) {
        this.songIds = songIds != null ? new ArrayList<>(songIds) : new ArrayList<>();
        this.updatedAt = Instant.now();
    }

    public void addSongId(String songId) {
        if (songId == null || songId.isBlank()) {
            return;
        }
        if (!songIds.contains(songId)) {
            songIds.add(songId);
            this.updatedAt = Instant.now();
        }
    }

    public void removeSongId(String songId) {
        if (songIds.remove(songId)) {
            this.updatedAt = Instant.now();
        }
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        this.updatedAt = Instant.now();
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return name == null || name.isBlank() ? "Untitled album" : name + " (" + songIds.size() + " songs)";
    }
}
