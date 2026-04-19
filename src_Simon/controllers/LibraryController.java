package controllers;

import models.Song;
import services.persistence.SongRepository;

import java.util.List;

/**
 * Contrôleur pour la navigation dans le catalogue musical.
 */
public class LibraryController {

    private SongRepository songRepo;

    public LibraryController(SongRepository songRepo) {
        this.songRepo = songRepo;
    }

    public List<Song> rechercherParTitre(String titre) {
        return songRepo.rechercherParTitre(titre);
    }

    public List<Song> rechercherParArtiste(String artiste) {
        return songRepo.rechercherParArtiste(artiste);
    }

    public List<Song> rechercherParGenre(String genre) {
        return songRepo.rechercherParGenre(genre);
    }

    public Song getDetails(String songId) {
        return songRepo.trouverParId(songId);
    }

    public List<Song> getTousMorceaux() {
        return songRepo.trouverTout();
    }
}
