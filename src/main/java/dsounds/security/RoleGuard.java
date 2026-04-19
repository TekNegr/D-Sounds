package dsounds.security;

import dsounds.controllers.AuthException;
import dsounds.models.AuthSession;
import dsounds.models.User;
import dsounds.models.UserRole;

/**
 * RoleGuard — point d'entrée unique pour toutes les vérifications de rôle et d'accès.
 *
 * <p>Chaque méthode {@code exiger*()} lève une {@link AuthException} si la condition
 * n'est pas remplie, ce qui permet d'interrompre proprement un traitement dans les
 * contrôleurs sans dupliquer la logique de vérification partout dans le code.</p>
 *
 * <p>Principe de défense en profondeur : les vérifications sont effectuées
 * <em>deux fois</em> — d'abord dans l'UI (désactivation des boutons via
 * {@code canXxx()}), puis dans les méthodes de contrôleur (via {@code requireXxx()}).
 * Ainsi, même si l'UI est contournée, l'action est bloquée.</p>
 *
 * <p><b>Auteur (Laksman)</b> — gestion des rôles et restrictions d'accès.</p>
 */
public final class RoleGuard {

    private RoleGuard() {
        // Classe utilitaire — instanciation interdite.
    }

    // =========================================================================
    // Méthodes de vérification avec exception (pour les contrôleurs)
    // =========================================================================

    /**
     * Vérifie que la session est ouverte (utilisateur connecté, quel que soit son rôle).
     *
     * @param session la session courante
     * @throws AuthException si aucun utilisateur n'est connecté
     */
    public static void requireAuthenticated(AuthSession session) throws AuthException {
        if (session == null || !session.isAuthenticated()) {
            throw new AuthException(
                    "Accès refusé : vous devez être connecté pour effectuer cette action.");
        }
    }

    /**
     * Vérifie que l'utilisateur connecté est un administrateur.
     *
     * @param session la session courante
     * @throws AuthException si l'utilisateur n'est pas ADMIN
     */
    public static void requireAdmin(AuthSession session) throws AuthException {
        requireAuthenticated(session);
        if (!session.hasRole(UserRole.ADMIN)) {
            throw new AuthException(
                    "Access denied: this action is restricted to administrators. "
                  + "Please log in with an administrator account.");
        }
    }

    /**
     * Vérifie que l'utilisateur connecté est un abonné (SUBSCRIBER).
     *
     * @param session la session courante
     * @throws AuthException si l'utilisateur n'est pas SUBSCRIBER
     */
    public static void requireSubscriber(AuthSession session) throws AuthException {
        requireAuthenticated(session);
        if (!session.hasRole(UserRole.SUBSCRIBER)) {
            throw new AuthException(
                    "Access denied: this action is restricted to subscribers. "
                  + "Create an account or log in to access all features.");
        }
    }

    /**
     * Vérifie que l'utilisateur connecté n'est pas un simple visiteur.
     * Autorise les abonnés (SUBSCRIBER) et les administrateurs (ADMIN).
     *
     * @param session la session courante
     * @throws AuthException si l'utilisateur est VISITOR ou non connecté
     */
    public static void requireNotVisitor(AuthSession session) throws AuthException {
        requireAuthenticated(session);
        if (session.hasRole(UserRole.VISITOR)) {
            throw new AuthException(
                    "Access denied: visitors cannot perform this action. "
                  + "Create a subscriber account to access all features.");
        }
    }

    /**
     * Vérifie que l'utilisateur courant peut modifier une ressource identifiée
     * par son propriétaire. Un administrateur peut tout modifier. Un abonné
     * ne peut modifier que ses propres ressources.
     *
     * @param session       la session courante
     * @param ownerUsername le nom d'utilisateur du propriétaire de la ressource
     * @param resourceName  le nom de la ressource (pour le message d'erreur)
     * @throws AuthException si l'accès est refusé
     */
    public static void requireAdminOrOwner(AuthSession session,
                                           String ownerUsername,
                                           String resourceName) throws AuthException {
        requireNotVisitor(session);

        if (session.hasRole(UserRole.ADMIN)) {
            return; // Admins ont tous les droits.
        }

        User current = session.getCurrentUser();
        if (current == null || ownerUsername == null
                || !current.getUsername().equalsIgnoreCase(ownerUsername.trim())) {
            throw new AuthException(
                    "Accès refusé : vous n'êtes pas propriétaire de " + resourceName
                  + ". Seul le propriétaire ou un administrateur peut effectuer cette action.");
        }
    }

    /**
     * Vérifie qu'un admin peut agir sur un compte cible.
     * Empêche toute modification du compte « admin » par défaut.
     *
     * @param session        la session courante
     * @param targetUsername le nom d'utilisateur de la cible
     * @throws AuthException si l'opération est refusée
     */
    public static void requireAdminOnAccount(AuthSession session,
                                              String targetUsername) throws AuthException {
        requireAdmin(session);
        if ("admin".equalsIgnoreCase(targetUsername != null ? targetUsername.trim() : "")) {
            throw new AuthException(
                    "Accès refusé : le compte administrateur par défaut ne peut pas être modifié.");
        }
    }

    // =========================================================================
    // Méthodes booléennes (pour l'UI — désactivation des boutons)
    // =========================================================================

    /**
     * @return {@code true} si la session appartient à un administrateur
     */
    public static boolean isAdmin(AuthSession session) {
        return session != null && session.hasRole(UserRole.ADMIN);
    }

    /**
     * @return {@code true} si la session appartient à un abonné (SUBSCRIBER)
     */
    public static boolean isSubscriber(AuthSession session) {
        return session != null && session.hasRole(UserRole.SUBSCRIBER);
    }

    /**
     * @return {@code true} si la session appartient à un visiteur ou n'est pas ouverte
     */
    public static boolean isVisitor(AuthSession session) {
        return session == null
                || !session.isAuthenticated()
                || session.hasRole(UserRole.VISITOR);
    }

    /**
     * Indique si l'utilisateur courant peut modifier une ressource donnée.
     * Admins : oui toujours. Subscribers : uniquement s'ils en sont propriétaires.
     * Visitors / non connectés : non.
     *
     * @param session       la session courante
     * @param ownerUsername le propriétaire de la ressource
     * @return {@code true} si la modification est autorisée
     */
    public static boolean canModify(AuthSession session, String ownerUsername) {
        if (session == null || !session.isAuthenticated()) {
            return false;
        }
        if (session.hasRole(UserRole.ADMIN)) {
            return true;
        }
        if (session.hasRole(UserRole.VISITOR)) {
            return false;
        }
        User current = session.getCurrentUser();
        return current != null && ownerUsername != null
                && current.getUsername().equalsIgnoreCase(ownerUsername.trim());
    }

    /**
     * Indique si l'utilisateur courant peut créer des playlists
     * (tout le monde sauf les visiteurs).
     *
     * @param session la session courante
     * @return {@code true} si la création de playlist est autorisée
     */
    public static boolean canCreatePlaylist(AuthSession session) {
        return !isVisitor(session);
    }

    /**
     * Indique si l'utilisateur courant peut administrer le catalogue
     * (ajouter/supprimer des morceaux, des albums, etc.).
     *
     * @param session la session courante
     * @return {@code true} si l'administration du catalogue est autorisée
     */
    public static boolean canManageCatalog(AuthSession session) {
        return isAdmin(session);
    }

    /**
     * Indique si l'utilisateur courant peut gérer des comptes utilisateurs
     * (suspendre, réactiver, supprimer).
     *
     * @param session la session courante
     * @return {@code true} si la gestion des comptes est autorisée
     */
    public static boolean canManageUsers(AuthSession session) {
        return isAdmin(session);
    }
}
