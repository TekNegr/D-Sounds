package dsounds.models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JamRoom model for synchronized listening sessions between multiple users.
 */
public class JamRoom {

    private String id;
    private String hostUserId;
    private String name;
    private String currentSongId;
    private long playbackPositionMs;
    private boolean isPlaying;
    private List<String> participantUserIds;
    private Instant createdAt;
    private Instant updatedAt;

    public JamRoom() {
        this.id = UUID.randomUUID().toString();
        this.participantUserIds = new ArrayList<>();
        this.playbackPositionMs = 0;
        this.isPlaying = false;
        this.createdAt = Instant.now();
    }

    public JamRoom(String hostUserId, String name) {
        this();
        this.hostUserId = hostUserId;
        this.name = name;
        this.participantUserIds.add(hostUserId);
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHostUserId() {
        return hostUserId;
    }

    public void setHostUserId(String hostUserId) {
        this.hostUserId = hostUserId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCurrentSongId() {
        return currentSongId;
    }

    public void setCurrentSongId(String currentSongId) {
        this.currentSongId = currentSongId;
    }

    public long getPlaybackPositionMs() {
        return playbackPositionMs;
    }

    public void setPlaybackPositionMs(long playbackPositionMs) {
        this.playbackPositionMs = playbackPositionMs;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
        this.updatedAt = Instant.now();
    }

    public List<String> getParticipantUserIds() {
        return participantUserIds;
    }

    public void setParticipantUserIds(List<String> participantUserIds) {
        this.participantUserIds = participantUserIds != null ? participantUserIds : new ArrayList<>();
    }

    public void addParticipant(String userId) {
        if (!participantUserIds.contains(userId)) {
            participantUserIds.add(userId);
            this.updatedAt = Instant.now();
        }
    }

    public void removeParticipant(String userId) {
        if (participantUserIds.remove(userId)) {
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

    @Override
    public String toString() {
        return name + " (" + participantUserIds.size() + " listening)";
    }
}
