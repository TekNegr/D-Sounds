## Getting Started

Welcome to the VS Code Java world. Here is a guideline to help you get started to write Java code in Visual Studio Code.

## Folder Structure

The workspace contains two folders by default, where:

- `src`: the folder to maintain sources
- `lib`: the folder to maintain dependencies

Meanwhile, the compiled output files will be generated in the `bin` folder by default.

> If you want to customize the folder structure, open `.vscode/settings.json` and update the related settings there.

## Dependency Management

The `JAVA PROJECTS` view allows you to manage your dependencies. More details can be found [here](https://github.com/microsoft/vscode-java-dependency#manage-dependencies).

## Prerequis : 

- JAVA 25 (**EXACTEMENT CETTE VERSION**)
- JavaFX SDK 25.0.2 (https://gluonhq.com/products/javafx/) (**IMPORTANT** : Il faudra connaitre le chemin d'installation de JavaFX SDK pour la configuration de lancement)
- IDE au choix 
    - Visual Studio : Merci de changer le chemin d'installation de JavaFX SDK dans le fichier `.vscode/launch.json` à la ligne 11
    - IntelliJ : Jsp mais vous etes des merdes à ne pas utiliser VSCode
    - Eclipse : //

## Lancer le projet :
- Visual Studio Code : F5 ou Ctrl + F5 ou Lancer Run depuis le debbugger ou en faisant clique droit (si vous avez l'extension)
- IntelliJ : Run
- Eclipse : Run

# Auteurs :
- Henintsoa RAMAKAVELO // Seigneur Malveillant
- Laksmann CHANDIRAKUMAR // Un mec vraiment bizarre
- Emilien CORNILLION // J'ai pas encore d'avis 
- Simon ELYNN // CancreMaxxing

## Features : 

### Demandé par le CDC : 
- [ ] Application de musique avec une interface graphique
- [ ] Visiteur simple :
    - [ ] Consulter le catalogue musical (Artistes, Albums, Chansons, Groupes, Genres)
    - [ ] Écouter un nombre limité de morceaux (nuuuuuuuul #OpenSourceLife)
    - [ ] Auth 
- [ ] Abonné :
    - [ ] Toutes les fonctionnalités du visiteur
    - [ ] Écouter tous les morceaux
    - [ ] Créer des playlists
    - [ ] Recommandations personnalisées
- [ ] Admin :
    - [ ] Toutes les fonctionnalités de l'abonné
    - [ ] Ajouter/Modifier/Supprimer des artistes, albums, chansons, groupes, genres
    - [ ] Gérer les abonnements (ajouter/supprimer des abonnés, gérer les paiements)
- [ ] Système de notation et de commentaires pour les morceaux
- [ ] Système de recherche avancée (par artiste, album, genre, etc.)
- [ ] Statistiques d'écoute pour les utilisateurs (top morceaux, artistes préférés, etc.) + statistiques globales (morceaux les plus écoutés, genres les plus populaires, etc.)
- [ ] Playlist collaborative entre abonnés
- [ ] Architecture MVC 
- [ ] Persistance des données (base de données ou fichiers)

*Rendement :*
- [ ] Diagramme de classes UML & Logique métier
- [ ] Diagramme MVC 
- [ ] Le rapport demande à voir le developpement de l'application, pas juste le résultat final. Il faut montrer les étapes de développement, les choix techniques, les difficultés rencontrées et comment elles ont été surmontées.
- [ ] Conception du GUI (maquettes -> implémentation)


### Extras : 

// A vous de voir, mais n'oubliez pas que le projet doit être fonctionnel et que les fonctionnalités demandées par le CDC doivent être implémentées avant d'ajouter des fonctionnalités supplémentaires.

- [ ] Animations et transitions dans l'interface graphique
- [ ] Intégration d'une API de streaming musical comme MusicAPI, mais ca peut etre payant(Spotify, Deezer, etc.) 
- [ ] Aboonées peuvent partager leurs musiques 
- [ ] Systeme de sync, Firebase ? 
