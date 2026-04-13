# Notes d'intégration

## Ce que couvre ce module dans le sujet
- Menu principal avec connexion admin / client, création de compte et mode visiteur
- Gestion des rôles utilisateur
- Sauvegarde / chargement des utilisateurs

## Intégration avec le reste du projet

### Catalogue
Après une connexion visiteur ou abonné, brancher les écrans de navigation catalogue.

### Abonné
Après connexion abonné, brancher :
- playlists
- historique d'écoute
- recommandations

### Admin
Après connexion admin, brancher :
- gestion du catalogue
- gestion des comptes
- statistiques

## Convention MVC conseillée
- Aucune logique d'auth dans les vues
- `AuthService` reste l'unique point d'entrée métier pour l'auth
- Les vues JavaFX appellent `AuthService` puis changent d'écran selon le rôle

## Fichiers JavaFX ajoutés
- `musicapp/app/JavaFxAuthApp.java`
- `musicapp/view/javafx/AuthRouter.java`
- `musicapp/view/javafx/LoginPane.java`
- `musicapp/view/javafx/SignupPane.java`
- `src/module-info.java`

## Ce que ton responsable GUI peut faire ensuite
- remplacer les vues programmatiques par du FXML si l'équipe le souhaite
- conserver `AuthService`, `Session` et `UtilisateurRepository` tels quels
- router vers des dashboards dédiés selon le rôle
