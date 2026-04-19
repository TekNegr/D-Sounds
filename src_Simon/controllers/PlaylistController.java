package controllers;

import models.CollaborativePlaylist;
import models.CollaborativePlaylist.Permission;
import models.Playlist;
import models.Song;
import models.User;
import services.persistence.PlaylistRepository;
import services.persistence.UserRepository;
import services.persistence.SongRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Contrôleur pour la gestion des playlists et playlists collaboratives.
 * Fait le lien entre le modèle et la vue pour toutes les opérations sur les playlists.
 */
public class PlaylistController {

    private PlaylistRepository playlistRepo;
    private UserRepository userRepo;
    private SongRepository songRepo;

    public PlaylistController(PlaylistRepository playlistRepo, UserRepository userRepo, SongRepository songRepo) {
        this.playlistRepo = playlistRepo;
        this.userRepo = userRepo;
        this.songRepo = songRepo;
    }

    // ===========================================================
    //  PLAYLISTS STANDARD
    // ===========================================================

    /**
     * Crée une nouvelle playlist pour un abonné.
     * @param nom le nom de la playlist
     * @param userId l'ID du propriétaire
     * @return la playlist créée
     * @throws IllegalStateException si l'utilisateur n'est pas abonné
     */
    public Playlist creerPlaylist(String nom, String userId) {
        User user = userRepo.trouverParId(userId);
        if (user == null || user.getRole() != User.Role.ABONNE) {
            throw new IllegalStateException("Seuls les abonnés peuvent créer des playlists.");
        }
        String id = "PL-" + System.currentTimeMillis();
        Playlist playlist = new Playlist(id, nom, userId);
        playlistRepo.sauvegarder(playlist);
        user.ajouterPlaylist(id);
        userRepo.sauvegarder(user);
        return playlist;
    }

    /**
     * Renomme une playlist existante.
     * @param playlistId l'ID de la playlist
     * @param nouveauNom le nouveau nom
     * @param userId l'ID de l'utilisateur qui renomme
     * @throws SecurityException si l'utilisateur n'est pas le propriétaire
     */
    public void renommerPlaylist(String playlistId, String nouveauNom, String userId) {
        Playlist playlist = playlistRepo.trouverParId(playlistId);
        if (playlist == null) throw new IllegalArgumentException("Playlist introuvable : " + playlistId);
        if (!playlist.getProprietaireId().equals(userId)) {
            throw new SecurityException("Seul le propriétaire peut renommer la playlist.");
        }
        playlist.setNom(nouveauNom);
        playlistRepo.sauvegarder(playlist);
    }

    /**
     * Supprime une playlist.
     * @param playlistId l'ID de la playlist
     * @param userId l'ID de l'utilisateur qui supprime
     */
    public void supprimerPlaylist(String playlistId, String userId) {
        Playlist playlist = playlistRepo.trouverParId(playlistId);
        if (playlist == null) throw new IllegalArgumentException("Playlist introuvable : " + playlistId);
        if (!playlist.getProprietaireId().equals(userId)) {
            throw new SecurityException("Seul le propriétaire peut supprimer la playlist.");
        }

        // Si collaborative, retirer des listes des collaborateurs
        if (playlist instanceof CollaborativePlaylist) {
            CollaborativePlaylist cp = (CollaborativePlaylist) playlist;
            for (String collabId : cp.getCollaborateurIds()) {
                User collab = userRepo.trouverParId(collabId);
                if (collab != null) {
                    collab.retirerCollabPlaylist(playlistId);
                    userRepo.sauvegarder(collab);
                }
            }
        }

        User user = userRepo.trouverParId(userId);
        if (user != null) {
            user.retirerPlaylist(playlistId);
            userRepo.sauvegarder(user);
        }
        playlistRepo.supprimer(playlistId);
    }

    /**
     * Ajoute un morceau à une playlist.
     * @param playlistId l'ID de la playlist
     * @param songId l'ID du morceau
     * @param userId l'ID de l'utilisateur qui fait l'action
     * @return true si le morceau a été ajouté
     */
    public boolean ajouterMorceau(String playlistId, String songId, String userId) {
        Playlist playlist = playlistRepo.trouverParId(playlistId);
        if (playlist == null) throw new IllegalArgumentException("Playlist introuvable.");
        Song song = songRepo.trouverParId(songId);
        if (song == null) throw new IllegalArgumentException("Morceau introuvable.");

        boolean resultat;
        if (playlist instanceof CollaborativePlaylist) {
            resultat = ((CollaborativePlaylist) playlist).ajouterMorceauParCollaborateur(songId, userId);
        } else {
            if (!playlist.getProprietaireId().equals(userId)) {
                throw new SecurityException("Seul le propriétaire peut modifier cette playlist.");
            }
            resultat = playlist.ajouterMorceau(songId);
        }

        if (resultat) {
            playlistRepo.sauvegarder(playlist);
        }
        return resultat;
    }

    /**
     * Retire un morceau d'une playlist.
     * @param playlistId l'ID de la playlist
     * @param songId l'ID du morceau
     * @param userId l'ID de l'utilisateur
     * @return true si le morceau a été retiré
     */
    public boolean retirerMorceau(String playlistId, String songId, String userId) {
        Playlist playlist = playlistRepo.trouverParId(playlistId);
        if (playlist == null) throw new IllegalArgumentException("Playlist introuvable.");

        boolean resultat;
        if (playlist instanceof CollaborativePlaylist) {
            resultat = ((CollaborativePlaylist) playlist).retirerMorceauParCollaborateur(songId, userId);
        } else {
            if (!playlist.getProprietaireId().equals(userId)) {
                throw new SecurityException("Seul le propriétaire peut modifier cette playlist.");
            }
            resultat = playlist.retirerMorceau(songId);
        }

        if (resultat) {
            playlistRepo.sauvegarder(playlist);
        }
        return resultat;
    }

    /**
     * Récupère toutes les playlists d'un utilisateur (possédées + collaboratives).
     * @param userId l'ID de l'utilisateur
     * @return la liste des playlists accessibles par l'utilisateur
     */
    public List<Playlist> getPlaylistsUtilisateur(String userId) {
        User user = userRepo.trouverParId(userId);
        if (user == null) return new ArrayList<>();

        List<Playlist> result = new ArrayList<>();
        for (String plId : user.getPlaylistIds()) {
            Playlist pl = playlistRepo.trouverParId(plId);
            if (pl != null) result.add(pl);
        }
        for (String plId : user.getCollabPlaylistIds()) {
            Playlist pl = playlistRepo.trouverParId(plId);
            if (pl != null) result.add(pl);
        }
        return result;
    }

    // ===========================================================
    //  PLAYLISTS COLLABORATIVES
    // ===========================================================

    /**
     * Crée une playlist collaborative.
     * @param nom le nom de la playlist
     * @param userId l'ID du propriétaire
     * @return la playlist collaborative créée
     */
    public CollaborativePlaylist creerPlaylistCollaborative(String nom, String userId) {
        User user = userRepo.trouverParId(userId);
        if (user == null || user.getRole() != User.Role.ABONNE) {
            throw new IllegalStateException("Seuls les abonnés peuvent créer des playlists collaboratives.");
        }
        String id = "CPL-" + System.currentTimeMillis();
        CollaborativePlaylist playlist = new CollaborativePlaylist(id, nom, userId);
        playlistRepo.sauvegarder(playlist);
        user.ajouterPlaylist(id);
        userRepo.sauvegarder(user);
        return playlist;
    }

    /**
     * Ajoute un collaborateur à une playlist collaborative.
     * @param playlistId l'ID de la playlist
     * @param collaborateurId l'ID de l'abonné à ajouter
     * @param permission le niveau de permission
     * @param userId l'ID du propriétaire qui ajoute
     * @return true si ajouté
     */
    public boolean ajouterCollaborateur(String playlistId, String collaborateurId, Permission permission, String userId) {
        Playlist pl = playlistRepo.trouverParId(playlistId);
        if (!(pl instanceof CollaborativePlaylist)) {
            throw new IllegalArgumentException("Cette playlist n'est pas collaborative.");
        }
        CollaborativePlaylist cp = (CollaborativePlaylist) pl;

        if (!cp.getProprietaireId().equals(userId)) {
            throw new SecurityException("Seul le propriétaire peut ajouter des collaborateurs.");
        }

        User collaborateur = userRepo.trouverParId(collaborateurId);
        if (collaborateur == null || collaborateur.getRole() != User.Role.ABONNE) {
            throw new IllegalArgumentException("Seuls les abonnés peuvent être collaborateurs.");
        }

        boolean resultat = cp.ajouterCollaborateur(collaborateurId, permission);
        if (resultat) {
            collaborateur.ajouterCollabPlaylist(playlistId);
            userRepo.sauvegarder(collaborateur);
            playlistRepo.sauvegarder(cp);
        }
        return resultat;
    }

    /**
     * Retire un collaborateur d'une playlist collaborative.
     * @param playlistId l'ID de la playlist
     * @param collaborateurId l'ID du collaborateur à retirer
     * @param userId l'ID du propriétaire
     * @return true si retiré
     */
    public boolean retirerCollaborateur(String playlistId, String collaborateurId, String userId) {
        Playlist pl = playlistRepo.trouverParId(playlistId);
        if (!(pl instanceof CollaborativePlaylist)) {
            throw new IllegalArgumentException("Cette playlist n'est pas collaborative.");
        }
        CollaborativePlaylist cp = (CollaborativePlaylist) pl;

        if (!cp.getProprietaireId().equals(userId)) {
            throw new SecurityException("Seul le propriétaire peut retirer des collaborateurs.");
        }

        boolean resultat = cp.retirerCollaborateur(collaborateurId);
        if (resultat) {
            User collaborateur = userRepo.trouverParId(collaborateurId);
            if (collaborateur != null) {
                collaborateur.retirerCollabPlaylist(playlistId);
                userRepo.sauvegarder(collaborateur);
            }
            playlistRepo.sauvegarder(cp);
        }
        return resultat;
    }

    /**
     * Modifie la permission d'un collaborateur.
     * @param playlistId l'ID de la playlist
     * @param collaborateurId l'ID du collaborateur
     * @param nouvellePermission la nouvelle permission
     * @param userId l'ID du propriétaire
     */
    public void modifierPermissionCollaborateur(String playlistId, String collaborateurId,
                                                 Permission nouvellePermission, String userId) {
        Playlist pl = playlistRepo.trouverParId(playlistId);
        if (!(pl instanceof CollaborativePlaylist)) {
            throw new IllegalArgumentException("Cette playlist n'est pas collaborative.");
        }
        CollaborativePlaylist cp = (CollaborativePlaylist) pl;

        if (!cp.getProprietaireId().equals(userId)) {
            throw new SecurityException("Seul le propriétaire peut modifier les permissions.");
        }

        if (cp.modifierPermission(collaborateurId, nouvellePermission)) {
            playlistRepo.sauvegarder(cp);
        } else {
            throw new IllegalArgumentException("Collaborateur introuvable dans cette playlist.");
        }
    }
}
