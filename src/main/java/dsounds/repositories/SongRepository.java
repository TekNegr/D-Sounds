package dsounds.repositories;

import dsounds.models.Song;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

/**
 * Repository for managing Song storage and caching.
 * Handles both local and remote song metadata.
 */
public final class SongRepository {

    private static final Path ROOT_STORAGE = Path.of("local_storage");
    private static final Path SONGS_DIR = ROOT_STORAGE.resolve("songs");
    private static final Path COVERS_DIR = ROOT_STORAGE.resolve("covers");
    private static final Path METADATA_DIR = ROOT_STORAGE.resolve("metadata");

    private SongRepository() {
    }

    /**
     * Load all songs from local cache/storage
     */
    public static List<Song> loadAllLocalSongs() throws IOException {
        List<Song> songs = new ArrayList<>();

        if (!Files.exists(METADATA_DIR)) {
            return songs;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(METADATA_DIR, "*.properties")) {
            for (Path metadataFile : stream) {
                Song song = loadSongFromMetadata(metadataFile);
                if (song != null) {
                    songs.add(song);
                }
            }
        }

        songs.sort(Comparator.comparing(Song::getUploadedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return songs;
    }

    /**
     * Load a single song from metadata file
     */
    private static Song loadSongFromMetadata(Path metadataFile) {
        Properties metadata = new Properties();
        try (InputStream input = Files.newInputStream(metadataFile)) {
            metadata.load(input);

            Song song = new Song(
                metadata.getProperty("id"),
                metadata.getProperty("title"),
                metadata.getProperty("artist"),
                metadata.getProperty("genre")
            );

            song.setAlbum(metadata.getProperty("album"));
            song.setDescription(metadata.getProperty("description"));
            song.setLyrics(metadata.getProperty("lyrics"));
            song.setOriginalFileName(metadata.getProperty("originalFileName"));
            song.setStoredFileName(metadata.getProperty("storedFileName"));
            song.setLocalStoragePath(metadata.getProperty("storedPath"));
            song.setCoverImagePath(metadata.getProperty("coverImagePath"));
            song.setCoverImageFileName(metadata.getProperty("coverImageFileName"));
            song.setPublisherUsername(metadata.getProperty("publisherUsername"));
            song.setMimeType(metadata.getProperty("mimeType", "audio/mpeg"));

            String uploadedAtStr = metadata.getProperty("uploadedAt");
            if (uploadedAtStr != null) {
                try {
                    song.setUploadedAt(Instant.parse(uploadedAtStr));
                } catch (Exception ex) {
                    song.setUploadedAt(Instant.now());
                }
            }

            String durationStr = metadata.getProperty("durationSeconds");
            if (durationStr != null) {
                try {
                    song.setDurationSeconds(Integer.parseInt(durationStr));
                } catch (NumberFormatException ex) {
                    song.setDurationSeconds(0);
                }
            }

            song.setStorageLocation(Song.StorageLocation.LOCAL);
            return song;
        } catch (IOException ex) {
            return null;
        }
    }

    /**
     * Save a song's metadata to local storage
     */
    public static void saveSongMetadata(Song song) throws IOException {
        Files.createDirectories(SONGS_DIR);
        Files.createDirectories(COVERS_DIR);
        Files.createDirectories(METADATA_DIR);

        Properties metadata = new Properties();
        metadata.setProperty("id", song.getId());
        metadata.setProperty("title", song.getTitle() != null ? song.getTitle() : "");
        metadata.setProperty("artist", song.getArtist() != null ? song.getArtist() : "");
        metadata.setProperty("genre", song.getGenre() != null ? song.getGenre() : "");
        metadata.setProperty("album", song.getAlbum() != null ? song.getAlbum() : "");
        metadata.setProperty("description", song.getDescription() != null ? song.getDescription() : "");
        metadata.setProperty("lyrics", song.getLyrics() != null ? song.getLyrics() : "");
        metadata.setProperty("originalFileName", song.getOriginalFileName() != null ? song.getOriginalFileName() : "");
        metadata.setProperty("storedFileName", song.getStoredFileName() != null ? song.getStoredFileName() : "");
        metadata.setProperty("storedPath", song.getLocalStoragePath() != null ? song.getLocalStoragePath() : "");
        metadata.setProperty("coverImagePath", song.getCoverImagePath() != null ? song.getCoverImagePath() : "");
        metadata.setProperty("coverImageFileName", song.getCoverImageFileName() != null ? song.getCoverImageFileName() : "");
        metadata.setProperty("publisherUsername", song.getPublisherUsername() != null ? song.getPublisherUsername() : "");
        metadata.setProperty("mimeType", song.getMimeType() != null ? song.getMimeType() : "audio/mpeg");
        metadata.setProperty("durationSeconds", String.valueOf(song.getDurationSeconds()));
        metadata.setProperty("uploadedAt", song.getUploadedAt().toString());

        Path metadataPath = METADATA_DIR.resolve(song.getId() + ".properties");
        try (OutputStream output = Files.newOutputStream(metadataPath)) {
            metadata.store(output, "Song metadata - can be synced to Azure");
        }
    }

    /**
     * Delete a song metadata and related local files.
     */
    public static void deleteSong(Song song) throws IOException {
        if (song == null) {
            return;
        }

        if (song.getStoredFileName() != null && !song.getStoredFileName().isBlank()) {
            Files.deleteIfExists(SONGS_DIR.resolve(song.getStoredFileName()));
        }

        if (song.getCoverImageFileName() != null && !song.getCoverImageFileName().isBlank()) {
            Files.deleteIfExists(COVERS_DIR.resolve(song.getCoverImageFileName()));
        }

        Path metadataPath = METADATA_DIR.resolve(song.getId() + ".properties");
        Files.deleteIfExists(metadataPath);
    }

    /**
     * Get the local songs directory
     */
    public static Path getSongsDirectory() throws IOException {
        Files.createDirectories(SONGS_DIR);
        return SONGS_DIR;
    }

    /**
     * Get the local cover images directory
     */
    public static Path getCoversDirectory() throws IOException {
        Files.createDirectories(COVERS_DIR);
        return COVERS_DIR;
    }
}
