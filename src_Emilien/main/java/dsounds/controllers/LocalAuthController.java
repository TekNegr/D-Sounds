package dsounds.controllers;

import dsounds.models.AuthSession;
import dsounds.models.User;
import dsounds.models.UserRole;
import dsounds.repositories.UserRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Local authentication flow adapted from src_laksman auth service.
 * OAuth providers are intentionally excluded from this controller.
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
        return user;
    }

    public User login(String username, String password) throws AuthException {
        validateUsername(username);
        validatePassword(password);

        User user = repository.findByUsername(username.trim());
        if (user == null) {
            throw new AuthException("User not found.");
        }
        if (!user.isActive()) {
            throw new AuthException("Account is suspended.");
        }
        if (!user.verifyPassword(password)) {
            throw new AuthException("Incorrect password.");
        }

        session.open(user);
        return user;
    }

    public User login(String username, String password, UserRole expectedRole) throws AuthException {
        User user = login(username, password);
        if (expectedRole != null && user.getRole() != expectedRole) {
            session.close();
            throw new AuthException("This account does not have the required role.");
        }
        return user;
    }

    public User loginOrCreateFromOAuth(String proposedUsername, String email) throws AuthException {
        String base = cleanOAuthUsername(proposedUsername);
        String finalUsername = base;
        int suffix = 1;

        while (repository.exists(finalUsername)) {
            User existing = repository.findByUsername(finalUsername);
            if (existing != null && existing.isActive()) {
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

    public void suspendUser(String username) throws AuthException {
        User user = findRequiredUser(username, "Unable to suspend: user not found.");
        if (user.getRole() == UserRole.ADMIN) {
            throw new AuthException("Default administrator cannot be suspended.");
        }
        user.suspend();
    }

    public void reactivateUser(String username) throws AuthException {
        User user = findRequiredUser(username, "Unable to reactivate: user not found.");
        user.reactivate();
    }

    public void deleteUser(String username) throws AuthException {
        User user = findRequiredUser(username, "Unable to delete: user not found.");
        if (user.getRole() == UserRole.ADMIN) {
            throw new AuthException("Default administrator cannot be deleted.");
        }

        repository.remove(user.getUsername());
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
            throw new AuthException("This username already exists.");
        }
    }

    private static void validateUsername(String username) throws AuthException {
        if (username == null || username.isBlank()) {
            throw new AuthException("Username is required.");
        }
    }

    private static void validateEmail(String email) throws AuthException {
        if (email == null || email.isBlank()) {
            throw new AuthException("Email is required.");
        }
        if (!email.contains("@")) {
            throw new AuthException("Email format is invalid.");
        }
    }

    private static void validatePassword(String password) throws AuthException {
        if (password == null || password.isBlank()) {
            throw new AuthException("Password is required.");
        }
        if (password.length() < 4) {
            throw new AuthException("Password must contain at least 4 characters.");
        }
    }

    private static String cleanOAuthUsername(String proposedUsername) throws AuthException {
        if (proposedUsername == null || proposedUsername.isBlank()) {
            throw new AuthException("Cannot create OAuth account without an identifier.");
        }
        String cleaned = proposedUsername.trim().toLowerCase()
                .replaceAll("[^a-z0-9._-]", "_")
                .replaceAll("_+", "_");
        if (cleaned.isBlank()) {
            throw new AuthException("Cannot derive a valid local username from OAuth profile.");
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
            throw new AuthException("Username is required.");
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
