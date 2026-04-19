package dsounds.security;

import dsounds.controllers.AuthException;
import dsounds.models.AuthSession;
import dsounds.models.Playlist;
import dsounds.models.User;
import dsounds.models.UserRole;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * OwnershipChecker — vérifications d'appartenance et d'accès collaboratif aux playlists.
 *
 * <p>Implémente trois niveaux d'accès :</p>
 * <ul>
 *   <li><b>Propriétaire</b> : lecture + modification complète + suppression.</li>
 *   <li><b>Collaborateur EDITOR</b> : peut ajouter et supprimer des morceaux.</li>
 *   <li><b>Collaborateur VIEWER</b> : lecture seule (playlist privée visible).</li>
 * </ul>
 *
 * <p>Le registre collaboratif est stocké en mémoire (par ID de playlist).
 * Pour la persistance, sérialiser cet objet ou déléguer au {@code PlaylistRepository}.</p>
 *
 * <p><b>Auteur (Laksman)</b> — ownership checks et gestion collaborative des playlists.</p>
 */
public final class OwnershipChecker {

    /**
     * Rôle d'un collaborateur sur une playlist partagée.
     */
    public enum CollabRole {
        /** Peut ajouter et supprimer des morceaux dans la playlist. */
        EDITOR,
        /** Peut seulement lire le contenu de la playlist privée. */
        VIEWER
    }

    /**
     * Registre des collaborateurs : playlistId → (username normalisé → CollabRole).
     * Un utilisateur absent de la map n'a aucun accès à une playlist privée.
     */
    private final Map<String, Map<String, CollabRole>> registry = new HashMap<>();

    // =========================================================================
    // Accès lecture
    // =========================================================================

    /**
     * Indique si l'utilisateur courant peut <b>lire</b> la playlist.
     * <ul>
     *   <li>Playlist publique : accessible par tous.</li>
     *   <li>Playlist privée : propriétaire, admins, collaborateurs (EDITOR ou VIEWER).</li>
     * </ul>
     *
     * @param session       la session courante
     * @param playlist      la playlist cible
     * @return {@code true} si la lecture est autorisée
     */
    public boolean canRead(AuthSession session, Playlist playlist) {
        if (playlist == null) {
            return false;
        }
        if (playlist.isPublic()) {
            return true; // Tout le monde peut lire une playlist publique.
        }
        // Playlist privée.
        if (session == null || !session.isAuthenticated()) {
            return false;
        }
        if (session.hasRole(UserRole.ADMIN)) {
            return true;
        }
        User current = session.getCurrentUser();
        if (current == null) {
            return false;
        }
        // Propriétaire.
        if (current.getUsername().equalsIgnoreCase(playlist.getOwnerUsername())) {
            return true;
        }
        // Collaborateur (EDITOR ou VIEWER).
        return getCollabRole(playlist.getId(), current.getUsername()) != null;
    }

    /**
     * Lève une {@link AuthException} si l'utilisateur ne peut pas lire la playlist.
     *
     * @param session  la session courante
     * @param playlist la playlist cible
     * @throws AuthException si l'accès en lecture est refusé
     */
    public void requireReadAccess(AuthSession session, Playlist playlist) throws AuthException {
        if (!canRead(session, playlist)) {
            String name = playlist != null ? playlist.getName() : "this playlist";
            throw new AuthException(
                    "Accès refusé : la playlist \"" + name
                  + "\" est privée. Vous n'avez pas la permission de la consulter.");
        }
    }

    // =========================================================================
    // Accès écriture (modification des morceaux)
    // =========================================================================

    /**
     * Indique si l'utilisateur courant peut <b>modifier les morceaux</b> de la playlist
     * (ajouter/supprimer des songs). Admins, propriétaire et collaborateurs EDITOR.
     *
     * @param session  la session courante
     * @param playlist la playlist cible
     * @return {@code true} si la modification est autorisée
     */
    public boolean canEdit(AuthSession session, Playlist playlist) {
        if (playlist == null || session == null || !session.isAuthenticated()) {
            return false;
        }
        if (session.hasRole(UserRole.VISITOR)) {
            return false;
        }
        if (session.hasRole(UserRole.ADMIN)) {
            return true;
        }
        User current = session.getCurrentUser();
        if (current == null) {
            return false;
        }
        if (current.getUsername().equalsIgnoreCase(playlist.getOwnerUsername())) {
            return true;
        }
        // Collaborateur EDITOR uniquement.
        return getCollabRole(playlist.getId(), current.getUsername()) == CollabRole.EDITOR;
    }

    /**
     * Lève une {@link AuthException} si l'utilisateur ne peut pas modifier la playlist.
     *
     * @param session  la session courante
     * @param playlist la playlist cible
     * @throws AuthException si la modification est refusée
     */
    public void requireEditAccess(AuthSession session, Playlist playlist) throws AuthException {
        if (!canEdit(session, playlist)) {
            String name = playlist != null ? playlist.getName() : "this playlist";
            throw new AuthException(
                    "Accès refusé : vous ne pouvez pas modifier la playlist \"" + name
                  + "\". Seul le propriétaire, un collaborateur éditeur ou un administrateur peut le faire.");
        }
    }

    // =========================================================================
    // Accès suppression / gestion des métadonnées
    // =========================================================================

    /**
     * Indique si l'utilisateur courant peut <b>supprimer ou renommer</b> la playlist.
     * Seuls le propriétaire et les admins le peuvent.
     *
     * @param session  la session courante
     * @param playlist la playlist cible
     * @return {@code true} si la suppression est autorisée
     */
    public boolean canDelete(AuthSession session, Playlist playlist) {
        if (playlist == null) {
            return false;
        }
        return RoleGuard.canModify(session, playlist.getOwnerUsername());
    }

    /**
     * Lève une {@link AuthException} si l'utilisateur ne peut pas supprimer la playlist.
     *
     * @param session  la session courante
     * @param playlist la playlist cible
     * @throws AuthException si la suppression est refusée
     */
    public void requireDeleteAccess(AuthSession session, Playlist playlist) throws AuthException {
        if (!canDelete(session, playlist)) {
            String name = playlist != null ? playlist.getName() : "this playlist";
            throw new AuthException(
                    "Access denied: only the owner or an administrator can delete the playlist \""
                  + name + "\".");
        }
    }

    // =========================================================================
    // Gestion des collaborateurs
    // =========================================================================

    /**
     * Ajoute ou met à jour le rôle d'un collaborateur sur une playlist.
     * Seul le propriétaire ou un admin peut appeler cette méthode.
     *
     * @param session         la session courante
     * @param playlist        la playlist cible
     * @param collaborator    le nom d'utilisateur du collaborateur
     * @param role            le rôle à attribuer (EDITOR ou VIEWER)
     * @throws AuthException si l'opération est refusée ou les paramètres sont invalides
     */
    public void addCollaborator(AuthSession session,
                                Playlist playlist,
                                String collaborator,
                                CollabRole role) throws AuthException {
        requireDeleteAccess(session, playlist); // Seul propriétaire/admin gère les collaborateurs.

        if (collaborator == null || collaborator.isBlank()) {
            throw new AuthException("Le nom d'utilisateur du collaborateur est obligatoire.");
        }
        if (role == null) {
            throw new AuthException("Un rôle (EDITOR ou VIEWER) doit être spécifié pour le collaborateur.");
        }
        String normalized = collaborator.trim().toLowerCase();
        String owner = playlist.getOwnerUsername();
        if (owner != null && owner.equalsIgnoreCase(collaborator.trim())) {
            throw new AuthException(
                    "Le propriétaire de la playlist ne peut pas être son propre collaborateur.");
        }

        registry.computeIfAbsent(playlist.getId(), id -> new HashMap<>())
                .put(normalized, role);
    }

    /**
     * Retire un collaborateur d'une playlist.
     *
     * @param session      la session courante
     * @param playlist     la playlist cible
     * @param collaborator le nom d'utilisateur du collaborateur à retirer
     * @throws AuthException si l'opération est refusée
     */
    public void removeCollaborator(AuthSession session,
                                   Playlist playlist,
                                   String collaborator) throws AuthException {
        requireDeleteAccess(session, playlist);

        if (collaborator == null || collaborator.isBlank()) {
            throw new AuthException("Le nom d'utilisateur du collaborateur est obligatoire.");
        }
        Map<String, CollabRole> collab = registry.get(playlist.getId());
        if (collab != null) {
            collab.remove(collaborator.trim().toLowerCase());
        }
    }

    /**
     * Retourne le rôle collaboratif d'un utilisateur sur une playlist,
     * ou {@code null} s'il n'est pas collaborateur.
     *
     * @param playlistId le UUID de la playlist
     * @param username   le nom d'utilisateur
     * @return le {@link CollabRole} ou {@code null}
     */
    public CollabRole getCollabRole(String playlistId, String username) {
        if (playlistId == null || username == null) {
            return null;
        }
        Map<String, CollabRole> collab = registry.get(playlistId);
        return collab == null ? null : collab.get(username.trim().toLowerCase());
    }

    /**
     * Retourne l'ensemble des noms de collaborateurs d'une playlist.
     *
     * @param playlistId le UUID de la playlist
     * @return ensemble non modifiable des usernames collaborateurs
     */
    public Set<String> getCollaborators(String playlistId) {
        Map<String, CollabRole> collab = registry.get(playlistId);
        return collab == null ? Collections.emptySet()
                              : Collections.unmodifiableSet(collab.keySet());
    }

    /**
     * Retourne une description lisible des collaborateurs d'une playlist.
     *
     * @param playlistId le UUID de la playlist
     * @return une chaîne multiligne ou "Aucun collaborateur."
     */
    public String describeCollaborators(String playlistId) {
        Map<String, CollabRole> collab = registry.get(playlistId);
        if (collab == null || collab.isEmpty()) {
            return "Aucun collaborateur.";
        }
        StringBuilder sb = new StringBuilder();
        collab.forEach((name, role) ->
                sb.append("  - ").append(name).append(" [").append(role).append("]\n"));
        return sb.toString().stripTrailing();
    }

    /**
     * Supprime toutes les entrées de collaboration d'une playlist.
     * À appeler lors de la suppression d'une playlist.
     *
     * @param playlistId le UUID de la playlist supprimée
     */
    public void clearCollaborators(String playlistId) {
        registry.remove(playlistId);
    }

    // =========================================================================
    // Persistance des collaborateurs (Laksman — correction persistance)
    // =========================================================================

    /**
     * Sérialise le registre des collaborateurs d'une playlist en chaîne
     * (format "user1:EDITOR,user2:VIEWER") et le stocke dans le model Playlist.
     *
     * @param playlist la playlist à synchroniser
     */
    public void syncToPlaylist(Playlist playlist) {
        if (playlist == null) return;
        Map<String, CollabRole> collab = registry.get(playlist.getId());
        if (collab == null || collab.isEmpty()) {
            playlist.setCollaborators(null);
            return;
        }
        StringBuilder sb = new StringBuilder();
        collab.forEach((name, role) -> {
            if (sb.length() > 0) sb.append(",");
            sb.append(name).append(":").append(role.name());
        });
        playlist.setCollaborators(sb.toString());
    }

    /**
     * Charge le registre des collaborateurs depuis le champ persisté de la Playlist.
     *
     * @param playlist la playlist depuis laquelle charger
     */
    public void loadFromPlaylist(Playlist playlist) {
        if (playlist == null) return;
        String data = playlist.getCollaborators();
        if (data == null || data.isBlank()) return;
        Map<String, CollabRole> collab = registry.computeIfAbsent(
                playlist.getId(), id -> new HashMap<>());
        for (String entry : data.split(",")) {
            String[] parts = entry.split(":");
            if (parts.length == 2) {
                try {
                    collab.put(parts[0].trim().toLowerCase(),
                               CollabRole.valueOf(parts[1].trim()));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }
}
