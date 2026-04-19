package dsounds.controllers;

import dsounds.models.AuthSession;
import dsounds.models.User;
import dsounds.models.UserRole;
import dsounds.repositories.UserRepository;
import dsounds.security.RoleGuard;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Local authentication flow — gestion des utilisateurs et des sessions.
 *
 * <p>Toutes les actions administratives (suspension, suppression) sont
 * protégées par {@link dsounds.security.RoleGuard} en plus des contrôles existants,
 * assurant une défense en profondeur même si l'appel vient hors de l'UI.</p>
 *
 * <p><b>Mis à jour par Laksman</b> — enforcement des rôles côté contrôleur.</p>
 */
public class LocalAuthController {

    private final UserRepository repository;
    private final AuthSession session;

    public LocalAuthController(UserRepository repository, AuthSession session) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.session = Objects.requireNonNull(session, "session");
    }

    public static LocalAuthController createDefault() {
        Path storage = Path.of("local_storage", "users", "users.bin");
        return new LocalAuthController(new UserRepository(storage), new AuthSession());
    }

    public void loadUsers() throws IOException, ClassNotFoundException {
        repository.load();
        createDefaultAdminIfMissing();
    }

    public void saveUsers() throws IOException {
        repository.save();
    }

    public User registerSubscriber(String username, String email, String password) throws AuthException {
        validateUsername(username);
        validateEmail(email);
        validatePassword(password);
        ensureUsernameAvailable(username);

        User user;
        try {
            user = new User(username, email);
            user.setRole(UserRole.SUBSCRIBER);
            user.setAuthProvider("local");
            user.setPassword(password);
        } catch (IllegalArgumentException ex) {
            throw new AuthException(ex.getMessage());
        }

        repository.add(user);
        // Persistance immédiate — survit à un crash (Laksman).
        try { repository.save(); } catch (IOException ignored) {}
        return user;
    }

    public User login(String username, String password) throws AuthException {
        validateUsername(username);
        validatePassword(password);

        User user = repository.findByUsername(username.trim());
        if (user == null) {
            throw new AuthException("Utilisateur introuvable.");
        }
        if (!user.isActive()) {
            throw new AuthException("Ce compte est suspendu.");
        }
        if (!user.verifyPassword(password)) {
            throw new AuthException("Mot de passe incorrect.");
        }

        session.open(user);
        return user;
    }

    public User login(String username, String password, UserRole expectedRole) throws AuthException {
        User user = login(username, password);
        if (expectedRole != null && user.getRole() != expectedRole) {
            session.close();
            throw new AuthException("Ce compte ne possède pas le rôle requis pour cet accès.");
        }
        return user;
    }

    public User loginOrCreateFromOAuth(String proposedUsername, String email) throws AuthException {
        String base = cleanOAuthUsername(proposedUsername);
        String finalUsername = base;
        int suffix = 1;

        while (repository.exists(finalUsername)) {
            User existing = repository.findByUsername(finalUsername);
            if (existing != null) {
                // Vérification suspension — un compte suspendu ne peut pas se connecter via OAuth (Laksman).
                if (!existing.isActive()) {
                    throw new AuthException(
                            "Ce compte est suspendu. Contactez un administrateur.");
                }
                session.open(existing);
                return existing;
            }
            finalUsername = base + suffix;
            suffix++;
        }

        User user = new User(finalUsername, fallbackOAuthEmail(finalUsername, email));
        user.setRole(UserRole.SUBSCRIBER);
        user.setAuthProvider("oauth");
        user.setPassword(UUID.randomUUID().toString());
        repository.add(user);
        // Persistance immédiate du nouveau compte OAuth (Laksman).
        try { repository.save(); } catch (IOException ignored) {}
        session.open(user);
        return user;
    }

    public void continueAsVisitor() {
        User visitor = new User("visitor", "visitor@local.invalid");
        visitor.setId("VISITOR-SESSION");
        visitor.setRole(UserRole.VISITOR);
        visitor.setAuthProvider("guest");
        visitor.setPassword(UUID.randomUUID().toString());
        session.open(visitor);
    }

    public void logout() {
        session.close();
    }

    /**
     * Suspend a user account.
     * Requires ADMIN role — enforced via {@link RoleGuard#requireAdmin(AuthSession)}
     * for defense-in-depth (in addition to UI-level checks).
     *
     * <b>Modifié par Laksman</b> — ajout de la vérification de rôle côté contrôleur.
     */
    public void suspendUser(String username) throws AuthException {
        RoleGuard.requireAdmin(session); // Defense-in-depth: verify admin role in controller.
        User user = findRequiredUser(username, "Impossible de suspendre : utilisateur introuvable.");
        if (user.getRole() == UserRole.ADMIN) {
            throw new AuthException("Le compte administrateur par défaut ne peut pas être suspendu.");
        }
        user.suspend();
        // Persistance immédiate (Laksman).
        try { repository.save(); } catch (IOException ignored) {}
    }

    /**
     * Reactivate a suspended user account.
     * Requires ADMIN role.
     *
     * <b>Modifié par Laksman</b> — ajout de la vérification de rôle côté contrôleur.
     */
    public void reactivateUser(String username) throws AuthException {
        RoleGuard.requireAdmin(session); // Defense-in-depth.
        User user = findRequiredUser(username, "Impossible de réactiver : utilisateur introuvable.");
        user.reactivate();
        // Persistance immédiate (Laksman).
        try { repository.save(); } catch (IOException ignored) {}
    }

    /**
     * Delete a user account permanently.
     * Requires ADMIN role — enforced here and in the UI layer.
     *
     * <b>Modifié par Laksman</b> — ajout de la vérification de rôle côté contrôleur.
     */
    public void deleteUser(String username) throws AuthException {
        RoleGuard.requireAdmin(session); // Defense-in-depth.
        User user = findRequiredUser(username, "Impossible de supprimer : utilisateur introuvable.");
        if (user.getRole() == UserRole.ADMIN) {
            throw new AuthException("Le compte administrateur par défaut ne peut pas être supprimé.");
        }

        repository.remove(user.getUsername());
        // Persistance immédiate (Laksman).
        try { repository.save(); } catch (IOException ignored) {}
        if (session.isAuthenticated()
                && session.getCurrentUser().getUsername().equalsIgnoreCase(user.getUsername())) {
            session.close();
        }
    }

    public List<User> listUsers() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(User::getRole)
                        .thenComparing(User::getUsername, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public AuthSession getSession() {
        return session;
    }

    private void ensureUsernameAvailable(String username) throws AuthException {
        if (repository.exists(username.trim())) {
            throw new AuthException("Cet identifiant est déjà utilisé.");
        }
    }

    private static void validateUsername(String username) throws AuthException {
        if (username == null || username.isBlank()) {
            throw new AuthException("L'identifiant est obligatoire.");
        }
    }

    private static void validateEmail(String email) throws AuthException {
        if (email == null || email.isBlank()) {
            throw new AuthException("L'adresse e-mail est obligatoire.");
        }
        if (!email.contains("@")) {
            throw new AuthException("Le format de l'adresse e-mail est invalide.");
        }
    }

    private static void validatePassword(String password) throws AuthException {
        if (password == null || password.isBlank()) {
            throw new AuthException("Le mot de passe est obligatoire.");
        }
        if (password.length() < 4) {
            throw new AuthException("Le mot de passe doit contenir au moins 4 caractères.");
        }
    }

    private static String cleanOAuthUsername(String proposedUsername) throws AuthException {
        if (proposedUsername == null || proposedUsername.isBlank()) {
            throw new AuthException("Impossible de créer un compte OAuth sans identifiant.");
        }
        String cleaned = proposedUsername.trim().toLowerCase()
                .replaceAll("[^a-z0-9._-]", "_")
                .replaceAll("_+", "_");
        if (cleaned.isBlank()) {
            throw new AuthException("Impossible de déduire un identifiant valide depuis le profil OAuth.");
        }
        if (cleaned.length() < 3) {
            cleaned = cleaned + "_user";
        }
        return cleaned;
    }

    private static String fallbackOAuthEmail(String username, String email) {
        if (email == null || email.isBlank()) {
            return username + "@oauth.local";
        }
        return email;
    }

    private User findRequiredUser(String username, String notFoundMessage) throws AuthException {
        if (username == null || username.isBlank()) {
            throw new AuthException("L'identifiant est obligatoire.");
        }
        User user = repository.findByUsername(username.trim());
        if (user == null) {
            throw new AuthException(notFoundMessage);
        }
        return user;
    }

    private void createDefaultAdminIfMissing() {
        if (!repository.exists("admin")) {
            User admin = new User("admin", "admin@dsounds.local");
            admin.setRole(UserRole.ADMIN);
            admin.setAuthProvider("local");
            admin.setPassword("root");
            repository.add(admin);
            return;
        }

        User admin = repository.findByUsername("admin");
        if (admin != null) {
            admin.setRole(UserRole.ADMIN);
            admin.setAuthProvider("local");
            admin.reactivate();
            admin.setPassword("root");
        }
    }
}
