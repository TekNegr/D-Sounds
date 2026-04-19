package dsounds;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

public final class LocalSongRepository {

    private static final Path ROOT_STORAGE = Path.of("local_storage");
    private static final Path METADATA_DIR = ROOT_STORAGE.resolve("metadata");

    private LocalSongRepository() {
    }

    public static List<LocalSong> loadAllSongs() throws IOException {
        List<LocalSong> songs = new ArrayList<>();

        if (!Files.exists(METADATA_DIR)) {
            return songs;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(METADATA_DIR, "*.properties")) {
            for (Path metadataFile : stream) {
                LocalSong song = loadSong(metadataFile);
                if (song != null) {
                    songs.add(song);
                }
            }
        }

        songs.sort(Comparator.comparing(LocalSong::getUploadedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return songs;
    }

    private static LocalSong loadSong(Path metadataFile) {
        Properties metadata = new Properties();
        try (InputStream input = Files.newInputStream(metadataFile)) {
            metadata.load(input);
            return new LocalSong(
                metadata.getProperty("id", ""),
                metadata.getProperty("title", ""),
                metadata.getProperty("artist", ""),
                metadata.getProperty("genre", ""),
                metadata.getProperty("description", ""),
                metadata.getProperty("lyrics", ""),
                metadata.getProperty("originalFileName", ""),
                metadata.getProperty("storedFileName", ""),
                metadata.getProperty("storedPath", ""),
                metadata.getProperty("uploadedAt", "")
            );
        } catch (IOException ex) {
            return null;
        }
    }
}
