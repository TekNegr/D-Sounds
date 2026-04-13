# État de la partie Auth

## Ce qui est maintenant couvert
- Connexion administrateur
- Connexion abonné
- Création de compte abonné
- Mode visiteur
- Gestion de session et déconnexion
- Vérification des rôles
- Persistance des utilisateurs
- Gestion des comptes abonnés par l'admin :
  - lister les utilisateurs
  - suspendre un compte
  - réactiver un compte
  - supprimer un compte
- Flux JavaFX par rôle :
  - écran admin
  - écran abonné
  - écran visiteur
- Flux console par rôle, avec mini menu admin

## Compte admin de test
- identifiant : admin
- mot de passe : admin1234

## Ce qu'il reste hors de ta partie
- Brancher l'écran visiteur au vrai catalogue
- Brancher l'écran abonné aux playlists / historique / recommandations
- Brancher l'écran admin à la vraie gestion du catalogue et aux statistiques
- Remplacer les écrans JavaFX temporaires par l'interface finale de l'équipe
- Ajouter des tests automatiques si l'équipe en prévoit

## Scénarios à tester avant rendu
1. Connexion admin avec `admin / admin1234`
2. Création d'un abonné
3. Connexion de cet abonné
4. Refus d'un identifiant déjà pris
5. Refus d'un mauvais mot de passe
6. Mode visiteur
7. Suspension d'un abonné par l'admin
8. Reconnexion refusée d'un compte suspendu
9. Réactivation du compte
10. Suppression du compte
11. Sauvegarde puis relance du programme


## Bonus ajouté
- Bouton Google OAuth dans l'écran JavaFX
- Bouton GitHub OAuth dans l'écran JavaFX
- Création automatique d'un compte abonné local si OAuth réussit
