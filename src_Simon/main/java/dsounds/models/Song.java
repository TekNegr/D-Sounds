package dsounds.models;

import java.time.Instant;
import java.util.UUID;

/**
 * Unified Song model for both Azure cloud storage and local caching.
 * Songs can be stored remotely in Azure Blob Storage or cached locally.
 */
public class Song {

    private String id;
    private String title;
    private String artist;
    private String album;
    private String genre;
    private String description;
    private String lyrics;
    private int durationSeconds;
    private String mimeType;
    private String originalFileName;
    private String storedFileName;
    private String localStoragePath;
    private String coverImagePath;
    private String coverImageFileName;
    private String publisherUsername;
    private String azureBlobPath;
    private StorageLocation storageLocation;
    private Instant uploadedAt;
    private Instant cachedAt;

    public enum StorageLocation {
        LOCAL,      // Stored in local_storage/
        AZURE,      // Stored in Azure Blob Storage
        BOTH        // Cached locally from Azure
    }

    public Song() {
        this.id = UUID.randomUUID().toString();
        this.uploadedAt = Instant.now();
        this.storageLocation = StorageLocation.LOCAL;
    }

    public Song(String id, String title, String artist, String genre) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.title = title;
        this.artist = artist;
        this.genre = genre;
        this.uploadedAt = Instant.now();
        this.storageLocation = StorageLocation.LOCAL;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLyrics() {
        return lyrics;
    }

    public void setLyrics(String lyrics) {
        this.lyrics = lyrics;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public void setStoredFileName(String storedFileName) {
        this.storedFileName = storedFileName;
    }

    public String getLocalStoragePath() {
        return localStoragePath;
    }

    public void setLocalStoragePath(String localStoragePath) {
        this.localStoragePath = localStoragePath;
    }

    public String getAzureBlobPath() {
        return azureBlobPath;
    }

    public void setAzureBlobPath(String azureBlobPath) {
        this.azureBlobPath = azureBlobPath;
    }

    public String getCoverImagePath() {
        return coverImagePath;
    }

    public void setCoverImagePath(String coverImagePath) {
        this.coverImagePath = coverImagePath;
    }

    public String getCoverImageFileName() {
        return coverImageFileName;
    }

    public void setCoverImageFileName(String coverImageFileName) {
        this.coverImageFileName = coverImageFileName;
    }

    public String getPublisherUsername() {
        return publisherUsername;
    }

    public void setPublisherUsername(String publisherUsername) {
        this.publisherUsername = publisherUsername;
    }

    public StorageLocation getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(StorageLocation storageLocation) {
        this.storageLocation = storageLocation;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public Instant getCachedAt() {
        return cachedAt;
    }

    public void setCachedAt(Instant cachedAt) {
        this.cachedAt = cachedAt;
    }

    public String getSummary() {
        String titleValue = isBlank(title) ? "Untitled" : title;
        String artistValue = isBlank(artist) ? "Unknown artist" : artist;
        String genreValue = isBlank(genre) ? "No genre" : genre;
        return titleValue + " • " + artistValue + " • " + genreValue;
    }

    public String getDetails() {
        StringBuilder builder = new StringBuilder();
        builder.append("ID: ").append(id).append('\n');
        builder.append("Title: ").append(emptyFallback(title, "Untitled")).append('\n');
        builder.append("Artist: ").append(emptyFallback(artist, "Unknown artist")).append('\n');
        builder.append("Album: ").append(emptyFallback(album, "No album")).append('\n');
        builder.append("Genre: ").append(emptyFallback(genre, "No genre")).append('\n');
        builder.append("Duration: ").append(durationSeconds > 0 ? durationSeconds + "s" : "Unknown").append('\n');
        builder.append("Storage: ").append(storageLocation).append('\n');
        builder.append("Publisher: ").append(emptyFallback(publisherUsername, "Unknown")).append('\n');
        builder.append("Cover: ").append(emptyFallback(coverImageFileName, "No cover")).append('\n');
        builder.append("Original file: ").append(emptyFallback(originalFileName, "N/A")).append('\n');
        builder.append("Uploaded at: ").append(uploadedAt != null ? uploadedAt : "N/A").append("\n\n");
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

    @Override
    public String toString() {
        return getSummary();
    }
}
