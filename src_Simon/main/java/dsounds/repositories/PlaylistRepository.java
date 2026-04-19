package dsounds.repositories;

import dsounds.models.Playlist;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public final class PlaylistRepository {

    private static final Path ROOT_STORAGE = Path.of("local_storage");
    private static final Path PLAYLISTS_DIR = ROOT_STORAGE.resolve("playlists");

    private PlaylistRepository() {
    }

    public static List<Playlist> loadAllPlaylists() throws IOException {
        List<Playlist> playlists = new ArrayList<>();

        if (!Files.exists(PLAYLISTS_DIR)) {
            return playlists;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(PLAYLISTS_DIR, "*.properties")) {
            for (Path playlistFile : stream) {
                Playlist playlist = loadPlaylist(playlistFile);
                if (playlist != null) {
                    playlists.add(playlist);
                }
            }
        }

        playlists.sort(Comparator.comparing(Playlist::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return playlists;
    }

    public static void savePlaylist(Playlist playlist) throws IOException {
        Files.createDirectories(PLAYLISTS_DIR);

        Properties metadata = new Properties();
        metadata.setProperty("id", safeValue(playlist.getId()));
        metadata.setProperty("userId", safeValue(playlist.getUserId()));
        metadata.setProperty("ownerUsername", safeValue(playlist.getOwnerUsername()));
        metadata.setProperty("name", safeValue(playlist.getName()));
        metadata.setProperty("description", safeValue(playlist.getDescription()));
        metadata.setProperty("visibility", playlist.getVisibility().name());
        metadata.setProperty("songs", String.join(",", playlist.getSongIds()));
        metadata.setProperty("createdAt", playlist.getCreatedAt() != null ? playlist.getCreatedAt().toString() : Instant.now().toString());
        metadata.setProperty("updatedAt", playlist.getUpdatedAt() != null ? playlist.getUpdatedAt().toString() : Instant.now().toString());

        // Persistance des collaborateurs (Laksman — correction persistance collaborative).
        if (playlist.getCollaborators() != null && !playlist.getCollaborators().isEmpty()) {
            metadata.setProperty("collaborators", playlist.getCollaborators());
        }

        Path metadataPath = PLAYLISTS_DIR.resolve(playlist.getId() + ".properties");
        try (OutputStream output = Files.newOutputStream(metadataPath)) {
            metadata.store(output, "Playlist metadata");
        }
    }

    public static void deletePlaylist(String playlistId) throws IOException {
        Files.createDirectories(PLAYLISTS_DIR);
        Path metadataPath = PLAYLISTS_DIR.resolve(playlistId + ".properties");
        Files.deleteIfExists(metadataPath);
    }

    private static Playlist loadPlaylist(Path metadataFile) {
        Properties metadata = new Properties();
        try (InputStream input = Files.newInputStream(metadataFile)) {
            metadata.load(input);

            Playlist playlist = new Playlist();
            playlist.setId(metadata.getProperty("id", ""));
            playlist.setUserId(metadata.getProperty("userId", ""));
            playlist.setOwnerUsername(metadata.getProperty("ownerUsername", ""));
            playlist.setName(metadata.getProperty("name", "Untitled playlist"));
            playlist.setDescription(metadata.getProperty("description", ""));

            String visibility = metadata.getProperty("visibility", "PUBLIC");
            try {
                playlist.setVisibility(Playlist.Visibility.valueOf(visibility));
            } catch (IllegalArgumentException ex) {
                playlist.setVisibility(Playlist.Visibility.PUBLIC);
            }

            String songs = metadata.getProperty("songs", "");
            if (songs.isBlank()) {
                playlist.setSongIds(new ArrayList<>());
            } else {
                playlist.setSongIds(Arrays.stream(songs.split(","))
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .collect(Collectors.toList()));
            }

            String createdAt = metadata.getProperty("createdAt");
            if (createdAt != null && !createdAt.isBlank()) {
                try {
                    playlist.setCreatedAt(Instant.parse(createdAt));
                } catch (RuntimeException ex) {
                    playlist.setCreatedAt(Instant.now());
                }
            }

            String updatedAt = metadata.getProperty("updatedAt");
            if (updatedAt != null && !updatedAt.isBlank()) {
                try {
                    playlist.setUpdatedAt(Instant.parse(updatedAt));
                } catch (RuntimeException ex) {
                    playlist.setUpdatedAt(playlist.getCreatedAt());
                }
            }

            // Chargement des collaborateurs persistés (Laksman).
            String collabs = metadata.getProperty("collaborators", "");
            if (!collabs.isBlank()) {
                playlist.setCollaborators(collabs);
            }

            return playlist;
        } catch (IOException ex) {
            return null;
        }
    }

    private static String safeValue(String value) {
        return value == null ? "" : value;
    }
}
