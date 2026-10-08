# HAI704I — TP1 : Java RMI (Cabinet Vétérinaire)

Projet réalisé dans le cadre du TP1 d'architectures distribuées (Master Informatique, 2026–2027).

## Structure du projet

Le projet est découpé en trois parties sans Maven ni modules Java :
- `common/` : interfaces distantes (`AnimalRemote`, `VetClinicRemote`, `MedicalRecordRemote`, `VetObserverRemote`) et types sérialisables (`Species`, `Dog`).
- `server/` : implémentations des objets distants (`VetClinicImpl`, `AnimalImpl`, `MedicalRecordImpl`) et classe principale `Server`.
- `client/` : interface console interactive (`Main`, `CliConsole`, `CliLogic`), observateur (`VetObserverImpl`) et tests (`Client`).

## Compilation

Nécessite Java 17+. Depuis la racine du projet :

```bash
mkdir -p common/out server/out client/out

javac -d common/out common/src/vet/common/*.java
javac -cp common/out -d server/out server/src/vet/server/*.java
javac -cp common/out -d client/out client/src/vet/client/*.java
```

## Lancement

### 1. Démarrer le serveur
Le serveur lance son propre registre RMI sur le port 1099 et publie le service `VetClinic` :

```bash
java -cp common/out:server/out vet.server.Server
```

### 2. Lancer le client interactif (CLI)
Dans un autre terminal :

```bash
java -cp common/out:client/out vet.client.Main
```

Le menu propose les options demandées :
1. Lister les patients
2. Rechercher un patient par son nom
3. Ajouter un nouveau patient
4. Consulter un dossier médical
5. Modifier l'état de santé
6. Ajouter une observation
7. S'abonner aux alertes
8. Se désabonner
9. Quitter (libère l'observateur pour terminer le processus)

---

## Validation des étapes

### Tests automatisés (A1 à A7)
Un script de test valide l'ensemble des points dans une même console :

```bash
java -cp common/out:client/out vet.client.Client
```

Ce test vérifie :
- **A1** : l'animal renvoyé par le cabinet est bien un stub RMI dynamique (`jdk.proxy...`).
- **A2** : `Species` est transmis par valeur (une modification de la copie locale côté client n'affecte pas l'objet sur le serveur).
- **A3** : `MedicalRecord` est un objet distant partagé.
- **A4** : le registre ne contient qu'une seule liaison (`VetClinic`), la recherche renvoie `null` si l'animal n'existe pas.
- **A5** : l'ajout d'un patient incrémente le compteur et l'animal est bien créé sur le serveur.
- **A7** : l'envoi d'une classe présente uniquement sur le client (`UnsharedSpecies`) lève une `ClassNotFoundException`, tandis que l'envoi de `Dog` (présent dans `common`) fonctionne.

### Validation multi-processus (A3 et A6)

Redémarrer le serveur avant chaque test (l'état est en mémoire : si le serveur a déjà plus de 99 patients, aucune alerte 100 ne peut plus être produite, et pour A3 une ancienne valeur identique ferait croire à tort que le test passe).

- **A3 (deux clients modifient et lisent le même dossier)** :
  ```bash
  # Terminal 1 : Client 1 met à jour le dossier puis se termine
  java -cp common/out:client/out vet.client.Client a3-writer Rex "En observation" "Visite de controle"

  # Terminal 2 : Client 2 (autre JVM) lit le dossier ; code retour 1 si la modification n'est pas vue
  java -cp common/out:client/out vet.client.Client a3-reader Rex "En observation" "Visite de controle"
  ```

- **A6 (deux observateurs + crash d'un client)** :
  ```bash
  # Terminaux 1 et 2 : deux observateurs (chaque alerte est affichée et écrite dans /tmp/vet_obs_<nom>.log)
  java -cp common/out:client/out vet.client.Client observer Observer-1 60
  java -cp common/out:client/out vet.client.Client observer Observer-2 60

  # Terminal 3 : atteindre 99 patients, puis franchir le seuil 100
  java -cp common/out:client/out vet.client.Client populate 99
  java -cp common/out:client/out vet.client.Client populate 100

  # Tuer brutalement un observateur (kill -9 <pid du premier java>), puis franchir 500
  java -cp common/out:client/out vet.client.Client populate 500
  ```
  Le serveur affiche `Observer unreachable, removing from subscriber list.` et Observer-2 reçoit l'alerte du seuil 500 (idem pour 1000 avec `populate 1000`).

---

## Remarques pour le compte-rendu

- **Seuils à la baisse (A6)** : Le serveur contient la logique de détection des franchissements à la baisse (`prevCount >= threshold && newCount < threshold`). Cependant, comme l'interface demandée par le sujet ne comporte pas de méthode de suppression de patient, le nombre d'animaux ne peut pas diminuer en utilisation normale.
- **Persistance** : Les données sont stockées en mémoire dans le tas du serveur (remise à zéro au redémarrage du serveur).
