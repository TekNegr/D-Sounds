package controllers;

import models.Song;
import models.User;
import services.persistence.SongRepository;
import services.persistence.UserRepository;

import java.util.List;

/**
 * Contrôleur pour la lecture de morceaux.
 * Gère les limites d'écoute pour les visiteurs et l'historique pour les abonnés.
 */
public class PlayerController {

    private SongRepository songRepo;
    private UserRepository userRepo;

    public PlayerController(SongRepository songRepo, UserRepository userRepo) {
        this.songRepo = songRepo;
        this.userRepo = userRepo;
    }

    /**
     * Simule l'écoute d'un morceau.
     * Vérifie les droits d'écoute, incrémente les compteurs.
     * @param songId l'ID du morceau à écouter
     * @param userId l'ID de l'utilisateur
     * @return le morceau écouté
     * @throws IllegalStateException si le visiteur a atteint sa limite d'écoutes
     */
    public Song ecouter(String songId, String userId) {
        Song song = songRepo.trouverParId(songId);
        if (song == null) throw new IllegalArgumentException("Morceau introuvable : " + songId);

        User user = userRepo.trouverParId(userId);
        if (user == null) throw new IllegalArgumentException("Utilisateur introuvable : " + userId);

        if (!user.peutEcouter()) {
            throw new IllegalStateException(
                "Limite d'écoutes atteinte (5 par session). Créez un compte pour écouter sans limite !"
            );
        }

        // Incrémenter les compteurs
        song.incrementerEcoutes();
        user.ajouterEcoute(songId);
        songRepo.sauvegarder(song);
        userRepo.sauvegarder(user);

        return song;
    }

    /**
     * Récupère l'historique d'écoute d'un utilisateur sous forme de morceaux.
     * @param userId l'ID de l'utilisateur
     * @return la liste des morceaux écoutés (peut contenir des doublons)
     */
    public List<String> getHistoriqueEcoute(String userId) {
        User user = userRepo.trouverParId(userId);
        if (user == null) throw new IllegalArgumentException("Utilisateur introuvable.");
        return user.getHistoriqueEcoute();
    }

    /**
     * Retourne le nombre d'écoutes restantes pour un visiteur.
     * @param userId l'ID du visiteur
     * @return le nombre d'écoutes restantes, ou -1 si illimité (abonné)
     */
    public int getEcoutesRestantes(String userId) {
        User user = userRepo.trouverParId(userId);
        if (user == null) return 0;
        if (user.getRole() != User.Role.VISITEUR) return -1; // illimité
        return Math.max(0, 5 - user.getEcoutesSession());
    }
}
