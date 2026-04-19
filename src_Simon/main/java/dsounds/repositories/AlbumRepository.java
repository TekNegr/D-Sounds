package dsounds.repositories;

import dsounds.models.Album;
import dsounds.models.Song;

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
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Collectors;

public final class AlbumRepository {

    private static final Path ROOT_STORAGE = Path.of("local_storage");
    private static final Path ALBUMS_DIR = ROOT_STORAGE.resolve("albums");

    private AlbumRepository() {
    }

    public static List<Album> loadAllAlbums() throws IOException {
        List<Album> albums = new ArrayList<>();

        if (!Files.exists(ALBUMS_DIR)) {
            return albums;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(ALBUMS_DIR, "*.properties")) {
            for (Path albumFile : stream) {
                Album album = loadAlbum(albumFile);
                if (album != null) {
                    albums.add(album);
                }
            }
        }

        albums.sort(Comparator.comparing(Album::getName, String.CASE_INSENSITIVE_ORDER));
        return albums;
    }

    public static List<String> loadAlbumNames() throws IOException {
        return loadAllAlbums().stream()
                .map(Album::getName)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    public static Album findAlbum(String albumName) throws IOException {
        if (albumName == null || albumName.isBlank()) {
            return null;
        }

        String normalized = normalize(albumName);
        for (Album album : loadAllAlbums()) {
            if (normalize(album.getName()).equals(normalized)) {
                return album;
            }
        }
        return null;
    }

    public static Album registerSong(String albumName, Song song) throws IOException {
        if (song == null || albumName == null || albumName.isBlank()) {
            return null;
        }

        Album album = findAlbum(albumName);
        if (album == null) {
            album = new Album(albumName.trim());
        }

        album.addSongId(song.getId());
        saveAlbum(album);
        return album;
    }

    public static void moveSong(String previousAlbumName, String newAlbumName, Song song) throws IOException {
        if (song == null) {
            return;
        }

        if (previousAlbumName != null && !previousAlbumName.isBlank()) {
            Album previousAlbum = findAlbum(previousAlbumName);
            if (previousAlbum != null) {
                previousAlbum.removeSongId(song.getId());
                saveAlbum(previousAlbum);
            }
        }

        if (newAlbumName != null && !newAlbumName.isBlank()) {
            registerSong(newAlbumName, song);
        }
    }

    public static void removeSongFromAlbum(String albumName, String songId) throws IOException {
        if (albumName == null || albumName.isBlank() || songId == null || songId.isBlank()) {
            return;
        }

        Album album = findAlbum(albumName);
        if (album == null) {
            return;
        }

        album.removeSongId(songId);
        saveAlbum(album);
    }

    public static void saveAlbum(Album album) throws IOException {
        if (album == null || album.getName() == null || album.getName().isBlank()) {
            return;
        }

        Files.createDirectories(ALBUMS_DIR);
        Properties metadata = new Properties();
        metadata.setProperty("id", safe(album.getId()));
        metadata.setProperty("name", safe(album.getName()));
        metadata.setProperty("description", safe(album.getDescription()));
        metadata.setProperty("songIds", String.join(",", album.getSongIds()));
        metadata.setProperty("createdAt", album.getCreatedAt() != null ? album.getCreatedAt().toString() : Instant.now().toString());
        metadata.setProperty("updatedAt", album.getUpdatedAt() != null ? album.getUpdatedAt().toString() : Instant.now().toString());

        Path albumPath = ALBUMS_DIR.resolve(safeFilePart(album.getName()) + ".properties");
        try (OutputStream output = Files.newOutputStream(albumPath)) {
            metadata.store(output, "Album metadata");
        }
    }

    private static Album loadAlbum(Path albumFile) {
        Properties metadata = new Properties();
        try (InputStream input = Files.newInputStream(albumFile)) {
            metadata.load(input);

            Album album = new Album();
            album.setId(metadata.getProperty("id", ""));
            album.setName(metadata.getProperty("name", ""));
            album.setDescription(metadata.getProperty("description", ""));

            String songIds = metadata.getProperty("songIds", "");
            if (songIds.isBlank()) {
                album.setSongIds(new ArrayList<>());
            } else {
                album.setSongIds(Arrays.stream(songIds.split(","))
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .collect(Collectors.toList()));
            }

            String createdAt = metadata.getProperty("createdAt");
            if (createdAt != null && !createdAt.isBlank()) {
                try {
                    album.setCreatedAt(Instant.parse(createdAt));
                } catch (RuntimeException ex) {
                    album.setCreatedAt(Instant.now());
                }
            }

            String updatedAt = metadata.getProperty("updatedAt");
            if (updatedAt != null && !updatedAt.isBlank()) {
                try {
                    album.setUpdatedAt(Instant.parse(updatedAt));
                } catch (RuntimeException ex) {
                    album.setUpdatedAt(album.getCreatedAt());
                }
            }

            return album;
        } catch (IOException ex) {
            return null;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String safeFilePart(String value) {
        return value == null ? "unknown" : value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
