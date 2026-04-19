package controllers;

import models.JamRoom;
import models.User;
import services.persistence.UserRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur pour la gestion des salles de jam collaboratives.
 */
public class RoomController {

    private Map<String, JamRoom> rooms;
    private UserRepository userRepo;

    public RoomController(UserRepository userRepo) {
        this.rooms = new HashMap<>();
        this.userRepo = userRepo;
    }

    /**
     * Crée une nouvelle salle de jam.
     * @param nom le nom de la salle
     * @param userId l'ID du créateur (doit être abonné)
     * @return la salle créée
     */
    public JamRoom creerRoom(String nom, String userId) {
        User user = userRepo.trouverParId(userId);
        if (user == null || user.getRole() != User.Role.ABONNE) {
            throw new IllegalStateException("Seuls les abonnés peuvent créer des salles de jam.");
        }
        String id = "ROOM-" + System.currentTimeMillis();
        JamRoom room = new JamRoom(id, nom, userId);
        rooms.put(id, room);
        return room;
    }

    /**
     * Rejoint une salle de jam.
     * @param roomId l'ID de la salle
     * @param userId l'ID de l'utilisateur
     * @return true si l'utilisateur a rejoint la salle
     */
    public boolean rejoindreRoom(String roomId, String userId) {
        JamRoom room = rooms.get(roomId);
        if (room == null) throw new IllegalArgumentException("Salle introuvable.");
        if (!room.isActive()) throw new IllegalStateException("La salle n'est plus active.");
        return room.rejoindre(userId);
    }

    /**
     * Quitte une salle de jam.
     * @param roomId l'ID de la salle
     * @param userId l'ID de l'utilisateur
     */
    public void quitterRoom(String roomId, String userId) {
        JamRoom room = rooms.get(roomId);
        if (room != null) room.quitter(userId);
    }

    public JamRoom getRoom(String roomId) { return rooms.get(roomId); }

    public List<JamRoom> getRoomsActives() {
        List<JamRoom> actives = new ArrayList<>();
        for (JamRoom r : rooms.values()) {
            if (r.isActive()) actives.add(r);
        }
        return actives;
    }
}
