package services.persistence;

import models.User;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository pour la persistance des utilisateurs.
 * Utilise la sérialisation Java.
 */
public class UserRepository {

    private static final String FICHIER = "data/users.dat";
    private Map<String, User> users;

    public UserRepository() {
        this.users = new HashMap<>();
        charger();
    }

    public void sauvegarder(User user) {
        users.put(user.getId(), user);
        persister();
    }

    public User trouverParId(String id) {
        return users.get(id);
    }

    /**
     * Trouve un utilisateur par son email.
     * @param email l'email recherché
     * @return l'utilisateur, ou null
     */
    public User trouverParEmail(String email) {
        for (User u : users.values()) {
            if (u.getEmail().equals(email)) return u;
        }
        return null;
    }

    /**
     * Trouve un utilisateur par son pseudo.
     * @param pseudo le pseudo recherché
     * @return l'utilisateur, ou null
     */
    public User trouverParPseudo(String pseudo) {
        for (User u : users.values()) {
            if (u.getPseudo().equalsIgnoreCase(pseudo)) return u;
        }
        return null;
    }

    public void supprimer(String id) {
        users.remove(id);
        persister();
    }

    public List<User> trouverTout() {
        return new ArrayList<>(users.values());
    }

    /** Retourne tous les abonnés */
    public List<User> trouverAbonnes() {
        List<User> result = new ArrayList<>();
        for (User u : users.values()) {
            if (u.getRole() == User.Role.ABONNE) result.add(u);
        }
        return result;
    }

    /** Retourne le nombre total d'utilisateurs */
    public int compter() {
        return users.size();
    }

    private void persister() {
        try {
            new File("data").mkdirs();
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FICHIER));
            oos.writeObject(users);
            oos.close();
        } catch (IOException e) {
            System.err.println("Erreur sauvegarde users : " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void charger() {
        File f = new File(FICHIER);
        if (!f.exists()) return;
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
            users = (Map<String, User>) ois.readObject();
            ois.close();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erreur chargement users : " + e.getMessage());
            users = new HashMap<>();
        }
    }
}
