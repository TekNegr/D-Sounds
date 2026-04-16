package dsounds.controllers;

import dsounds.models.Song;

import java.util.Locale;

public final class SongSearchUtils {

    private SongSearchUtils() {
    }

    public static boolean matches(Song song, String query) {
        if (song == null) {
            return false;
        }

        String normalizedQuery = normalize(query);
        if (normalizedQuery.isBlank()) {
            return true;
        }

        return contains(song.getTitle(), normalizedQuery)
                || contains(song.getArtist(), normalizedQuery)
                || contains(song.getGenre(), normalizedQuery)
                || contains(song.getPublisherUsername(), normalizedQuery)
                || contains(song.getAlbum(), normalizedQuery);
    }

    private static boolean contains(String value, String query) {
        return normalize(value).contains(query);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
