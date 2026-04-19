package dsounds.models;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * User model for authentication and user profiles.
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String username;
    private String email;
    private String passwordHash;
    private String displayName;
    private String bio;
    private UserRole role;
    private boolean active;
    private String authProvider;
    private Instant createdAt;
    private Instant updatedAt;

    public User() {
        this.id = UUID.randomUUID().toString();
        this.role = UserRole.SUBSCRIBER;
        this.active = true;
        this.authProvider = "local";
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public User(String username, String email) {
        this();
        setUsername(username);
        setEmail(email);
        this.displayName = this.username;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = Objects.requireNonNull(id, "id").trim();
        touch();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required.");
        }
        this.username = username.trim();
        if (displayName == null || displayName.isBlank()) {
            this.displayName = this.username;
        }
        touch();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }
        this.email = email.trim().toLowerCase(Locale.ROOT);
        touch();
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash is required.");
        }
        this.passwordHash = passwordHash;
        touch();
    }

    public void setPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 4) {
            throw new IllegalArgumentException("Password must have at least 4 characters.");
        }
        this.passwordHash = hash(rawPassword);
        touch();
    }

    public boolean verifyPassword(String attemptedPassword) {
        if (attemptedPassword == null || passwordHash == null || passwordHash.isBlank()) {
            return false;
        }
        return passwordHash.equals(hash(attemptedPassword));
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName == null ? null : displayName.trim();
        touch();
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
        touch();
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role == null ? UserRole.SUBSCRIBER : role;
        touch();
    }

    public boolean isActive() {
        return active;
    }

    public void suspend() {
        this.active = false;
        touch();
    }

    public void reactivate() {
        this.active = true;
        touch();
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = (authProvider == null || authProvider.isBlank())
                ? "local"
                : authProvider.trim().toLowerCase(Locale.ROOT);
        touch();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        touch();
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable.", e);
        }
    }

    @Override
    public String toString() {
        return username;
    }
}
