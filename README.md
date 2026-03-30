# 🎵 MusicApp – Application de Musique Java

Bienvenue sur MusicApp, une application de musique moderne avec interface graphique réalisée en Java et JavaFX. Le but ? Offrir aux utilisateurs une expérience riche : écouter, découvrir, gérer leur musique et profiter d’un système d’abonnement complet.


## 👨‍💻 Auteurs

- Henintsoa RAMAKAVELO // Seigneur Malveillant
- Laksmann CHANDIRAKUMAR // Homme Bizarre
- Emilien CORNILLION // Cornichon
- Simon ELYNN // CancreMaxxing
---

## 🚀 Prise en main rapide

### Prérequis

- **Java 25** _(obligatoire, version exacte)_
- **JavaFX SDK 25.0.2**\
  [Télécharger ici](https://gluonhq.com/products/javafx/).\
  > **Note :** Pense à bien repérer le chemin d’installation du SDK pour la configuration du lancement local.
- **IDE au choix** (VS Code, IntelliJ, Eclipse)

---

## 📦 Structure du projet

```
.
├── src/         # Code source Java (MVC structuré)
├── lib/         # Dépendances externes
├── bin/         # Fichiers compilés générés
├── .vscode/     # Configuration de VS Code
└── README.md    
```

---

## ⚙️ Configuration & Lancement

### Visual Studio Code

1. Ouvre le dossier du projet dans VS Code.
2. Modifie le chemin de JavaFX si besoin dans `.vscode/launch.json` (ligne 11).
3. Lance : `F5`, `Ctrl+F5` ou via le menu "Run".

### IntelliJ IDEA

- Ajoute la librairie JavaFX dans les paramètres du projet.
- Lance via le bouton "Run".

### Eclipse

- Ajoute JavaFX aux Build Path et VM Arguments.
- Lance via "Run".

> **Tip :** Si tu utilises VS Code, la configuration est déjà majoritairement prête. Pour les autres IDEs, l’ajout manuel de JavaFX peut être nécessaire.

---

## 🎯 Fonctionnalités principales

- **Catalogue musical complet** : Artistes, Albums, Chansons, Groupes, Genres
- **Visiteur** :
  - Navigation libre dans le catalogue
  - Limitation d’écoute sur certains morceaux
  - Authentification de base
- **Abonné** :
  - Accès illimité, création de playlists, recommandations personnalisées
- **Admin** :
  - Gestion avancée des contenus et utilisateurs, gestion des abonnements & paiements
- **Système d’avis** : Notation et commentaires sur les morceaux
- **Recherche intelligente** : Par artiste, album, genre, etc.
- **Statistiques d’écoute** (par utilisateur et globales)
- **Playlist collaborative** entre abonnés
- **Architecture MVC**
- **Persistance** (base de données ou fichier selon configuration)

---

## 📝 Livrables & développement

- Diagrammes UML (classes, logique métier, architecture MVC)
- Maquettes et conception du GUI
- Rapport détaillant :
    - Les étapes du développement
    - Les choix techniques
    - Les difficultés rencontrées et leur résolution

---

## 🌟 Fonctionnalités bonus possibles

- Animations & transitions dans l’UI
- Intégration avec une API de streaming externe (Spotify, Deezer, etc.)
- Partage de playlists entre abonnés
- Synchronisation cloud (ex: via Firebase)

---

## 📚 Ressources utiles

- [Documentation JavaFX](https://openjfx.io/)
- [VS Code Java - Setup Guide](https://code.visualstudio.com/docs/java/java-tutorial)
- [Gestion des dépendances Java](https://github.com/microsoft/vscode-java-dependency#manage-dependencies)

---

## 🔗 Remarques

- Assure-toi d’installer **exactement** les versions demandées (Java 25 et JavaFX 25.0.2) pour éviter tout problème de compatibilité.
- Avant d’ajouter tes propres features, veille à terminer l’ensemble des fonctionnalités demandées par le cahier des charges.

---

Bon code et bonne écoute ! 🎶