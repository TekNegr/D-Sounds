package dsounds.repositories;

import dsounds.models.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class UserRepository {

    private final Path storagePath;
    private final Map<String, User> usersByUsername;

    public UserRepository(Path storagePath) {
        this.storagePath = Objects.requireNonNull(storagePath, "storagePath");
        this.usersByUsername = new HashMap<>();
    }

    public void add(User user) {
        usersByUsername.put(normalize(user.getUsername()), user);
    }

    public boolean exists(String username) {
        if (username == null) {
            return false;
        }
        return usersByUsername.containsKey(normalize(username));
    }

    public User findByUsername(String username) {
        if (username == null) {
            return null;
        }
        return usersByUsername.get(normalize(username));
    }

    public Collection<User> findAll() {
        return Collections.unmodifiableCollection(usersByUsername.values());
    }

    public void remove(String username) {
        if (username != null) {
            usersByUsername.remove(normalize(username));
        }
    }

    public void save() throws IOException {
        if (storagePath.getParent() != null) {
            Files.createDirectories(storagePath.getParent());
        }
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(storagePath))) {
            out.writeObject(usersByUsername);
        }
    }

    @SuppressWarnings("unchecked")
    public void load() throws IOException, ClassNotFoundException {
        if (!Files.exists(storagePath)) {
            return;
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(storagePath))) {
            Object data = in.readObject();
            usersByUsername.clear();
            usersByUsername.putAll((Map<String, User>) data);
        }
    }

    private static String normalize(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
