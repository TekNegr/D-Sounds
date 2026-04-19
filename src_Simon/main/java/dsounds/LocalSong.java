package dsounds;

public class LocalSong {

    private final String id;
    private final String title;
    private final String artist;
    private final String genre;
    private final String description;
    private final String lyrics;
    private final String originalFileName;
    private final String storedFileName;
    private final String storedPath;
    private final String uploadedAt;

    public LocalSong(String id, String title, String artist, String genre, String description, String lyrics,
            String originalFileName, String storedFileName, String storedPath, String uploadedAt) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.genre = genre;
        this.description = description;
        this.lyrics = lyrics;
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.storedPath = storedPath;
        this.uploadedAt = uploadedAt;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getGenre() {
        return genre;
    }

    public String getDescription() {
        return description;
    }

    public String getLyrics() {
        return lyrics;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public String getStoredPath() {
        return storedPath;
    }

    public String getUploadedAt() {
        return uploadedAt;
    }

    public String getSummary() {
        String titleValue = isBlank(title) ? "Untitled" : title;
        String artistValue = isBlank(artist) ? "Unknown artist" : artist;
        String genreValue = isBlank(genre) ? "No genre" : genre;
        return titleValue + " • " + artistValue + " • " + genreValue;
    }

    public String getDetails() {
        StringBuilder builder = new StringBuilder();
        builder.append("Title: ").append(emptyFallback(title, "Untitled")).append('\n');
        builder.append("Artist: ").append(emptyFallback(artist, "Unknown artist")).append('\n');
        builder.append("Genre: ").append(emptyFallback(genre, "No genre")).append('\n');
        builder.append("Original file: ").append(emptyFallback(originalFileName, "N/A")).append('\n');
        builder.append("Stored file: ").append(emptyFallback(storedFileName, "N/A")).append('\n');
        builder.append("Stored path: ").append(emptyFallback(storedPath, "N/A")).append('\n');
        builder.append("Uploaded at: ").append(emptyFallback(uploadedAt, "N/A")).append("\n\n");
        builder.append("Description:\n").append(emptyFallback(description, "No description")).append("\n\n");
        builder.append("Lyrics:\n").append(emptyFallback(lyrics, "No lyrics"));
        return builder.toString();
    }

    private static String emptyFallback(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
