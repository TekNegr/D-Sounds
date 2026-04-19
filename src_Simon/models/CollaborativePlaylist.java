package models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Playlist collaborative permettant à plusieurs abonnés de contribuer.
 * Étend Playlist en ajoutant la gestion des collaborateurs et leurs droits.
 */
public class CollaborativePlaylist extends Playlist implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Permissions possibles pour un collaborateur.
     * LECTURE : peut seulement voir la playlist
     * AJOUT : peut ajouter des morceaux
     * AJOUT_SUPPRESSION : peut ajouter et supprimer des morceaux
     */
    public enum Permission {
        LECTURE, AJOUT, AJOUT_SUPPRESSION
    }

    // Map : userId -> Permission du collaborateur
    private Map<String, Permission> collaborateurs;

    /**
     * Crée une playlist collaborative.
     * @param id identifiant unique
     * @param nom nom de la playlist
     * @param proprietaireId ID du propriétaire (créateur)
     */
    public CollaborativePlaylist(String id, String nom, String proprietaireId) {
        super(id, nom, proprietaireId);
        this.collaborateurs = new HashMap<>();
    }

    // ---- Gestion des collaborateurs ----

    /**
     * Ajoute un collaborateur avec une permission donnée.
     * Le propriétaire ne peut pas être ajouté comme collaborateur.
     * @param userId l'ID de l'abonné à ajouter
     * @param permission le niveau de permission accordé
     * @return true si ajouté, false si c'est le propriétaire ou déjà collaborateur
     */
    public boolean ajouterCollaborateur(String userId, Permission permission) {
        if (userId.equals(getProprietaireId())) return false;
        if (collaborateurs.containsKey(userId)) return false;
        collaborateurs.put(userId, permission);
        return true;
    }

    /**
     * Retire un collaborateur de la playlist.
     * @param userId l'ID du collaborateur à retirer
     * @return true si retiré, false si non trouvé
     */
    public boolean retirerCollaborateur(String userId) {
        return collaborateurs.remove(userId) != null;
    }

    /**
     * Modifie la permission d'un collaborateur existant.
     * @param userId l'ID du collaborateur
     * @param nouvellePermission la nouvelle permission
     * @return true si modifié, false si le collaborateur n'existe pas
     */
    public boolean modifierPermission(String userId, Permission nouvellePermission) {
        if (!collaborateurs.containsKey(userId)) return false;
        collaborateurs.put(userId, nouvellePermission);
        return true;
    }

    /**
     * Vérifie si un utilisateur est collaborateur.
     * @param userId l'ID de l'utilisateur
     * @return true si l'utilisateur est collaborateur
     */
    public boolean estCollaborateur(String userId) {
        return collaborateurs.containsKey(userId);
    }

    /**
     * Récupère la permission d'un collaborateur.
     * @param userId l'ID du collaborateur
     * @return la permission, ou null si pas collaborateur
     */
    public Permission getPermission(String userId) {
        return collaborateurs.get(userId);
    }

    /**
     * Vérifie si un utilisateur peut ajouter un morceau.
     * Le propriétaire peut toujours ajouter.
     * @param userId l'ID de l'utilisateur
     * @return true si autorisé
     */
    public boolean peutAjouter(String userId) {
        if (userId.equals(getProprietaireId())) return true;
        Permission perm = collaborateurs.get(userId);
        return perm == Permission.AJOUT || perm == Permission.AJOUT_SUPPRESSION;
    }

    /**
     * Vérifie si un utilisateur peut supprimer un morceau.
     * Le propriétaire peut toujours supprimer.
     * @param userId l'ID de l'utilisateur
     * @return true si autorisé
     */
    public boolean peutSupprimer(String userId) {
        if (userId.equals(getProprietaireId())) return true;
        Permission perm = collaborateurs.get(userId);
        return perm == Permission.AJOUT_SUPPRESSION;
    }

    /**
     * Ajoute un morceau si l'utilisateur a la permission.
     * @param songId l'ID du morceau
     * @param userId l'ID de l'utilisateur qui fait l'action
     * @return true si le morceau a été ajouté
     * @throws SecurityException si l'utilisateur n'a pas la permission
     */
    public boolean ajouterMorceauParCollaborateur(String songId, String userId) {
        if (!peutAjouter(userId)) {
            throw new SecurityException("L'utilisateur " + userId + " n'a pas la permission d'ajouter des morceaux.");
        }
        return ajouterMorceau(songId);
    }

    /**
     * Retire un morceau si l'utilisateur a la permission.
     * @param songId l'ID du morceau
     * @param userId l'ID de l'utilisateur qui fait l'action
     * @return true si le morceau a été retiré
     * @throws SecurityException si l'utilisateur n'a pas la permission
     */
    public boolean retirerMorceauParCollaborateur(String songId, String userId) {
        if (!peutSupprimer(userId)) {
            throw new SecurityException("L'utilisateur " + userId + " n'a pas la permission de supprimer des morceaux.");
        }
        return retirerMorceau(songId);
    }

    /** Retourne l'ensemble des IDs des collaborateurs */
    public Set<String> getCollaborateurIds() {
        return collaborateurs.keySet();
    }

    /** Retourne la map complète des collaborateurs et permissions */
    public Map<String, Permission> getCollaborateurs() {
        return new HashMap<>(collaborateurs);
    }

    /** Retourne le nombre de collaborateurs */
    public int getNombreCollaborateurs() {
        return collaborateurs.size();
    }

    @Override
    public String toString() {
        return getNom() + " [collaborative, " + getNombreMorceaux() + " morceaux, " 
               + collaborateurs.size() + " collaborateurs]";
    }
}
