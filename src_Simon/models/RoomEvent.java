package models;

import java.io.Serializable;

/**
 * Événement survenu dans une JamRoom (arrivée, départ, pattern joué...).
 */
public class RoomEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type { REJOINDRE, QUITTER, PATTERN_JOUE, MESSAGE }

    private String roomId;
    private String userId;
    private Type type;
    private String contenu;
    private String timestamp;

    public RoomEvent(String roomId, String userId, Type type, String contenu) {
        this.roomId = roomId;
        this.userId = userId;
        this.type = type;
        this.contenu = contenu;
        this.timestamp = java.time.LocalDateTime.now().toString();
    }

    public String getRoomId() { return roomId; }
    public String getUserId() { return userId; }
    public Type getType() { return type; }
    public String getContenu() { return contenu; }
    public String getTimestamp() { return timestamp; }
}
