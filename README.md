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

### Programme de validation (A1 à A7)
La classe `Client` valide l'ensemble des points dans une même console :

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

## Bilan des exigences (A0 à A8)

| Exigence | Statut | Commentaire |
|---|---|---|
| **A0** | **PARTIAL** | Exercices tutoriels / de référence (Hello RMI mono puis multi-projet). Le dépôt implémente directement l'architecture cible à 3 projets (`common`, `server`, `client`). |
| **A1** | **PASS** | Récupération d'un patient sous forme de stub dynamique RMI (`Proxy.isProxyClass(...) == true`). |
| **A2** | **PASS** | `Species` est sérialisable et transmis par valeur (copie indépendante côté client). |
| **A3** | **PASS** | `MedicalRecordRemote` est un objet distant partagé (modifié par une JVM, relu par une seconde JVM indépendante). |
| **A4** | **PASS** | Seul `VetClinic` est publié dans le registre RMI. Recherche d'un animal inconnu renvoie `null`. |
| **A5** | **PASS** | Ajout dynamique d'un patient via `addPatient(...)` avec mise à jour de la liste sur le serveur. |
| **A6** | **PARTIAL** | Les franchissements à la hausse (100, 500, 1000) et la tolérance aux pannes des observateurs déconnectés sont validés expérimentalement. La logique de franchissement à la baisse est implémentée mais non déclenchable via l'API spécifiée `VetClinicRemote` faute d'opération de suppression de patient. |
| **A7** | **PASS** | Échec (`ClassNotFoundException`) lors du passage d'une sous-classe présente uniquement sur le client (`UnsharedSpecies`), succès pour une sous-classe présente dans `common` (`Dog`). |
| **A8** | **PASS** | CLI interactive complète avec découplage strict : `CliConsole` n'a aucun import ni dépendance envers `java.rmi.*` ; toutes les invocations et exceptions distantes sont encapsulées dans `CliLogic`. |

---

## Remarques pour le compte-rendu

- **A0 (Exercices tutoriels préliminaires)** : Les exercices HelloWorld préliminaires constituent des étapes d'initiation et de référence. Ce dépôt se concentre sur le livrable final attendu pour le cabinet vétérinaire, structuré d'emblée selon l'architecture cible à 3 dossiers (`common/`, `server/`, `client/`).
- **A6 (Seuils et observateurs)** :
  - *Franchissements à la hausse (100, 500, 1000)* : testés et validés avec plusieurs observateurs connectés.
  - *Gestion des observateurs défaillants* : lorsqu'un client observateur est tué brutalement (`kill -9`), l'exception `RemoteException` levée lors de la notification est capturée par le serveur, qui désabonne proprement l'observateur mort sans bloquer les autres abonnés.
  - *Franchissements à la baisse* : la logique est implémentée dans `VetClinicImpl.checkThresholds()` (`prevCount >= threshold && newCount < threshold`), mais n'est pas déclenchable via l'interface publique `VetClinicRemote` qui ne spécifie aucune méthode de suppression de patient (`removePatient`).
- **A8 (Séparation Console / RMI)** : La classe `CliConsole` ne contient aucun import `java.rmi.*` et ne manipule aucun stub distant. Elle consomme des vues/DTOs fournis par `CliLogic`, et les erreurs réseau/RMI sont transformées en `ClientException` pour un affichage convivial dans la console.
- **Persistance** : Les données sont stockées en mémoire dans le tas du serveur (remise à zéro au redémarrage du serveur).
