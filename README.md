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

Pour tester avec de vrais processus clients séparés :

- **A3 (deux clients modifient et lisent le même dossier)** :
  ```bash
  ./scripts/test_a3_multiprocess.sh
  ```
  Le premier processus client modifie le dossier de Rex et termine. Le second client le lit depuis une autre JVM et constate la modification.

- **A6 (deux observateurs + crash d'un client)** :
  ```bash
  ./scripts/test_a6_multiprocess.sh
  ```
  Lance deux clients observateurs en arrière-plan, ajoute le 100ᵉ patient (les deux reçoivent l'alerte), tue brutalement le premier client (`kill -9`), puis ajoute des patients jusqu'à 500 : le serveur retire le client mort sans planter et le deuxième client reçoit l'alerte.

---

## Remarques pour le compte-rendu

- **Seuils à la baisse (A6)** : Le serveur contient la logique de détection des franchissements à la baisse (`prevCount >= threshold && newCount < threshold`). Cependant, comme l'interface demandée par le sujet ne comporte pas de méthode de suppression de patient, le nombre d'animaux ne peut pas diminuer en utilisation normale.
- **Persistance** : Les données sont stockées en mémoire dans le tas du serveur (remise à zéro au redémarrage du serveur).
