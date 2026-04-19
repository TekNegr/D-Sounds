# ⚠ Conservation des données entre versions

## Problème
À chaque nouvelle version du ZIP, le dossier `local_storage/` (qui contient
vos morceaux, comptes et playlists) n'est pas inclus dans le ZIP car il
est dans `.gitignore`.

## Solution — copier local_storage entre versions

Quand vous recevez un nouveau ZIP :

1. **Avant** d'extraire le nouveau ZIP, copiez votre dossier `local_storage/`
   quelque part en sécurité (ex: Bureau).

2. Extrayez le nouveau ZIP.

3. **Collez** votre ancien `local_storage/` dans le dossier `D-Sounds/` du
   nouveau projet (au même niveau que `src/` et `pom.xml`).

## Emplacement exact
```
D-Sounds/
├── src/
├── pom.xml
├── local_storage/     ← copiez ce dossier d'une version à l'autre
│   ├── songs/         (fichiers MP3)
│   ├── metadata/      (infos des morceaux)
│   ├── covers/        (pochettes)
│   ├── playlists/     (vos playlists)
│   ├── reviews/       (avis)
│   └── users/         (comptes utilisateurs)
```

## Dans IntelliJ
IntelliJ exécute le projet depuis le dossier `D-Sounds/`, donc
`local_storage/` doit être directement dedans (pas dans `src/`).
