# Module Auth - MusicApp

Ce module couvre la partie **authentification / rôles / session / persistance** du sujet.

## Ce qui est inclus
- Connexion administrateur
- Connexion abonné
- Création de compte abonné
- Continuer en visiteur
- Gestion des rôles (`VISITEUR`, `ABONNE`, `ADMIN`)
- Session utilisateur
- Persistance par sérialisation Java
- Vue console
- Vue JavaFX simple pour login / inscription

## Structure
- `musicapp/model` : entités métier (`Utilisateur`, `Abonne`, `Admin`, `Visiteur`, `Role`, `Session`)
- `musicapp/controller` : logique d'auth (`AuthService`, `AuthException`)
- `musicapp/persistence` : sauvegarde / chargement (`UtilisateurRepository`)
- `musicapp/view` : vue console
- `musicapp/view/javafx` : vue JavaFX
- `musicapp/app/Main.java` : point d'entrée console
- `musicapp/app/JavaFxAuthApp.java` : point d'entrée JavaFX

## Comptes de test
Admin par défaut :
- identifiant : `admin`
- mot de passe : `admin1234`

## Lancer en console
Exécuter `musicapp.app.Main`

## Lancer en JavaFX sur IntelliJ
Pré-requis :
- Java 25
- JavaFX SDK 25.0.2

### VM options
```bash
--module-path "C:\chemin\vers\javafx-sdk-25.0.2\lib" --add-modules javafx.controls
```

### Classe principale
`musicapp.app.JavaFxAuthApp`

## Remarque
La compilation JavaFX n'a pas pu être vérifiée dans l'environnement de test ici, car le conteneur ne dispose pas d'une JVM assez récente pour les binaires JavaFX 25. Le code est prévu pour être compilé et lancé localement avec **Java 25**.
