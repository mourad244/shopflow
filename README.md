# TP2 - Mockito, AssertJ et TDD

Solution du TP2 ShopFlow du Pr. Youssef Gahi : prise de commande avec des
doublures Mockito et calcul des points de fidélité développé en TDD.

Ce dossier `shopflow-tp2` est un dépôt indépendant créé à partir du tag `tp1`.
Le TP1 d'origine reste dans `../tes_unitaire_master`. Le nom Maven du projet
reste `ma.shopflow:shopflow`, comme dans le sujet.

## 1. Exécuter la solution

Dans le terminal, depuis ce dossier :

```bash
./tester.sh
```

Le script utilise Java 21 et Maven fournis par IntelliJ sur ce poste, sans
modifier la configuration du système. Si `JAVA_HOME` est déjà défini, il reste
prioritaire et doit pointer vers un JDK 21 ou ultérieur.

Avec Java 21 et Maven 3.9 installés dans le `PATH`, la commande équivalente est
`mvn test`. Le premier lancement télécharge les dépendances et demande Internet.
Résultat attendu :

```text
Tests run: 42, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Pour lancer un groupe précis :

```bash
./tester.sh -Dtest=ServiceCommandeTest test
./tester.sh -Dtest=CalculateurFideliteTest test
```

Les tests ne contactent aucun entrepôt, service bancaire, serveur d'e-mails ni
base de données : ces quatre frontières sont remplacées par des mocks.
Le message `WARNING` indiquant « Serveur SMTP injoignable » est attendu dans le
test qui simule une panne d'e-mail. La commande reste confirmée et le test passe.

## 2. Les fichiers à lire

| Emplacement | Contenu |
| --- | --- |
| `src/main/java/ma/shopflow/panier/` | Les 5 classes métier du TP1 |
| `src/main/java/ma/shopflow/commande/` | Les objets de commande, les 4 interfaces et `ServiceCommande` |
| `src/main/java/ma/shopflow/fidelite/` | `CalculateurFidelite` et `NiveauFidelite` |
| `src/test/java/ma/shopflow/panier/` | `PanierTest` et `CodePromoTest` |
| `src/test/java/ma/shopflow/commande/` | `ServiceCommandeTest` |
| `src/test/java/ma/shopflow/fidelite/` | `CalculateurFideliteTest` |
| `ANOMALIES.md` | Les 2 anomalies du TP1 et les 2 du TP2 |
| `TDD.md` | Journal des vrais cycles rouge/vert/refactor |

Le `pom.xml` reprend les versions du sujet : JUnit 5.14.4, Mockito 5.23.0,
AssertJ 3.27.7, Surefire 3.5.6 et Dependency Plugin 3.11.0.
Le dernier plugin fournit le chemin du JAR Mockito à Surefire, qui le charge
avec `-javaagent` au démarrage des tests.

## 3. Comprendre la partie Mockito

Le vrai `ServiceCommande` reçoit quatre interfaces dans son constructeur.
Mockito crée leurs doublures avec `@Mock`, puis les injecte dans le service
avec `@InjectMocks`. Le panier et les objets métier restent des objets réels.

| Outil | Utilité dans les tests |
| --- | --- |
| `when(...).thenReturn(...)` | Programmer une disponibilité ou une réponse bancaire |
| `thenAnswer(...)` | Faire renvoyer au dépôt la commande qu'il reçoit |
| `verify(...)` | Contrôler un appel et ses arguments |
| `never()` | Vérifier qu'une opération n'a jamais eu lieu |
| `verifyNoInteractions(...)` | Vérifier qu'un service n'a pas été contacté |
| `InOrder` | Vérifier l'ordre réservation, débit, enregistrement, notification |
| `ArgumentCaptor` | Inspecter la commande créée à l'intérieur du service |
| `doThrow(...).when(...)` | Simuler une panne sur une méthode `void` |
| `assertThat(...)` | Écrire les assertions lisibles avec AssertJ |

Les programmations de mocks sont faites dans le test qui en a besoin, pour
respecter le mode strict de Mockito. Une programmation inutilisée fait échouer
le test avec `UnnecessaryStubbingException`.

Les six cas de commande correspondent aux règles du sujet :

| Test | Comportement vérifié |
| --- | --- |
| C1 : panier vide | Refus sans contacter aucun service |
| C2 : rupture de stock | Refus avant toute réservation et tout paiement |
| C3 et C4 : ordre des appels | Vérifier, réserver, débiter, enregistrer puis notifier |
| C4 : contenu enregistré | Client, lignes, total de 270 DH, statut et référence bancaire |
| C5 : paiement refusé | Libérer les réservations, lever l'exception, ne rien enregistrer |
| C6 : panne d'e-mail | Garder la commande confirmée et journaliser l'erreur |

Les deux défauts du code hérité ont d'abord été révélés par les tests, puis
corrigés. Leur cause, le résultat observé et le correctif sont dans `ANOMALIES.md`.

## 4. Comprendre la partie TDD

Le calculateur de fidélité a été construit en écrivant chaque test avant le code
correspondant. Les étapes sont conservées dans les commits et décrites dans
`TDD.md` :

1. **Premier test :** 100 DH donnent 10 points. La classe n'existe pas encore ;
   le test ne compile pas. Une première implémentation renvoie simplement 10.
2. **Triangulation :** cinq autres montants forcent le vrai calcul des tranches
   entières de 10 DH, avec `BigDecimal` et `RoundingMode.DOWN`.
3. **GOLD :** ajouter le doublement des points, puis déplacer le multiplicateur
   dans l'énumération et supprimer la duplication du calcul.
4. **Montant négatif :** ajouter le test, constater l'échec, puis ajouter la garde
   et son message d'exception.
5. **Plafond :** vérifier les trois cas de l'exercice, limiter les points après
   multiplication, puis nommer la constante `PLAFOND_PAR_COMMANDE`.

Un cycle rouge doit échouer pour la raison prévue. Le vert ajoute le minimum
nécessaire. Le refactor améliore la structure en gardant les tests au vert.

## 5. Répartition des 42 cas

| Classe de test | Cas exécutés |
| --- | ---: |
| `PanierTest` | 18 |
| `CodePromoTest` | 7 |
| `ServiceCommandeTest` | 6 |
| `CalculateurFideliteTest` | 11 |
| **Total** | **42** |

Les 11 cas de fidélité sont : le premier exemple à 100 DH, les cinq valeurs de
F1, le client GOLD, le montant négatif, et les trois cas du plafond. JUnit compte
chaque ligne de `@CsvSource` comme une exécution distincte.

## 6. Git et remise du TP

Les tags `tp1` et `tp2` permettent de retrouver les deux versions. Pour consulter
l'historique et les cycles TDD sans changer la version de travail :

```bash
git tag --list
git log --all --graph --oneline --decorate
git diff tp1..tp2 --stat
git status --short
```

Le livrable est une archive ZIP contenant les sources, les tests, les documents
et le dossier `.git` avec les deux tags, en excluant `target/`. Les rapports du
dernier lancement restent consultables localement dans `target/surefire-reports/`.

Les extensions de fin de sujet (fake en mémoire, photographie du panier et bonus
de première commande F5) sont facultatives et ne font pas partie des 42 cas demandés.
