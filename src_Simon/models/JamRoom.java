package models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Représente une salle de jam collaborative en temps réel.
 * Plusieurs abonnés peuvent rejoindre une room et jouer ensemble.
 */
public class JamRoom implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String nom;
    private String hoteId;              // ID du créateur de la room
    private List<String> participantIds; // IDs des participants connectés
    private boolean active;
    private int maxParticipants;

    public JamRoom(String id, String nom, String hoteId) {
        this.id = id;
        this.nom = nom;
        this.hoteId = hoteId;
        this.participantIds = new ArrayList<>();
        this.participantIds.add(hoteId);
        this.active = true;
        this.maxParticipants = 8;
    }

    public String getId() { return id; }
    public String getNom() { return nom; }
    public String getHoteId() { return hoteId; }
    public List<String> getParticipantIds() { return participantIds; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    /**
     * Un abonné rejoint la room.
     * @param userId l'ID de l'abonné
     * @return true si ajouté, false si room pleine ou déjà présent
     */
    public boolean rejoindre(String userId) {
        if (participantIds.contains(userId)) return false;
        if (participantIds.size() >= maxParticipants) return false;
        participantIds.add(userId);
        return true;
    }

    /**
     * Un participant quitte la room.
     * @param userId l'ID du participant
     */
    public void quitter(String userId) {
        participantIds.remove(userId);
        if (participantIds.isEmpty()) {
            active = false;
        }
    }

    public int getNombreParticipants() { return participantIds.size(); }

    @Override
    public String toString() {
        return nom + " (" + participantIds.size() + "/" + maxParticipants + " participants)";
    }
}
