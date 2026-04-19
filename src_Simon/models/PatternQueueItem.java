package models;

import java.io.Serializable;

/**
 * Élément dans la file d'attente de patterns d'une JamRoom.
 */
public class PatternQueueItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private String patternId;
    private String userId;
    private int position;

    public PatternQueueItem(String patternId, String userId, int position) {
        this.patternId = patternId;
        this.userId = userId;
        this.position = position;
    }

    public String getPatternId() { return patternId; }
    public String getUserId() { return userId; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}
