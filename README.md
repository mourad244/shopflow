# TP3 - Spring Boot, PostgreSQL et tests d'intégration

ShopFlow prolonge les TP1 et TP2 du Pr. Youssef Gahi avec une API REST,
un catalogue PostgreSQL, la persistance des commandes et des tests d'intégration.
Le test concurrent révèle une survente, puis vérifie sa correction.

Ce dépôt indépendant `shopflow-tp3` a été créé à partir du tag `tp2`.
Les projets `../tes_unitaire_master` et `../shopflow-tp2` restent disponibles.
Les tags `tp1`, `tp2` et l'historique des cycles TDD sont conservés.

## 1. Ouvrir le projet dans VS Code

Ouvrir le dossier `shopflow-tp3` avec **Fichier > Ouvrir le dossier**, puis attendre
l'import du projet Maven. Le `pom.xml` utilise Spring Boot **4.1.1** et Java **21** ;
le parent Spring Boot gère les versions de ses bibliothèques.
Les versions résolues sont JUnit **6.0.3**, Mockito **5.23.0**, AssertJ **3.27.7**
et Testcontainers **2.0.5**, consignées dans `validation/tp3/09-versions-dependances.log`.

Sur le poste où le TP a été préparé :

- VS Code, les extensions Java et **Spring Boot Extension Pack** sont installés.
- Le JDK 21 est disponible dans `/opt/intellij-idea-ultimate/jbr`.
- Maven 3.9 est disponible dans
  `/opt/intellij-idea-ultimate/plugins/maven/lib/maven3/bin/mvn`.
- Docker fonctionne et l'image `postgres:17-alpine` est déjà téléchargée.

Les réglages propres à ce poste sont dans `.vscode/settings.json` : JavaSE-21,
chemin de Maven, `JAVA_HOME` et `PATH` des nouveaux terminaux. Ce dossier est
ignoré par Git et exclu du livrable. Sur un autre poste, installer un JDK 21,
Maven 3.9, Docker et les extensions Java/Spring, puis adapter les chemins locaux.
Aucune base PostgreSQL permanente n'est nécessaire pour suivre ce guide.

Dans un **nouveau terminal** de VS Code, on peut vérifier l'environnement :

```bash
java -version
mvn -version
docker version
```

Le serveur Docker doit être accessible. Si l'image n'est pas encore présente :

```bash
docker pull postgres:17-alpine
```

## 2. Exécuter les tests

Depuis la racine du projet :

```bash
./tester.sh
```

Sans argument, le script lance `test`. Sur ce poste, il retrouve le JDK 21 et
Maven fournis par IntelliJ sans changer les réglages du système. Si `JAVA_HOME`
est déjà défini, il reste prioritaire et doit désigner un JDK compatible.
Avec Java 21 et Maven dans le `PATH`, la commande équivalente est `mvn test`.

La vérification complète enregistrée dans
[`06-validation-complete.log`](validation/tp3/06-validation-complete.log) a été lancée avec :

```bash
./tester.sh -B -ntp clean test
```

Résultat observé :

```text
Tests run: 53, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Pour travailler sur une partie, garder le but Maven `test` à la fin :

```bash
./tester.sh -Dtest=ProduitJpaRepositoryTest test
./tester.sh -Dtest=StockJpaAdapterTest test
./tester.sh -Dtest=CommandeApiTest test
./tester.sh -Dtest=ServiceCommandeTest test
./tester.sh -Dtest=CalculateurFideliteTest test
```

Par exemple, `mvn -Dtest=StockJpaAdapterTest test` est la commande Maven équivalente.
Les 42 cas des TP1/TP2 peuvent être lancés seuls :

```bash
./tester.sh -Dtest=PanierTest,CodePromoTest,ServiceCommandeTest,CalculateurFideliteTest test
```

| Classe de test | Cas | Ce qui est vérifié |
| --- | ---: | --- |
| `PanierTest` | 18 | Règles du panier, remise et livraison |
| `CodePromoTest` | 7 | Validité des codes promotionnels |
| `ServiceCommandeTest` | 6 | Service métier isolé avec Mockito |
| `CalculateurFideliteTest` | 11 | Points de fidélité développés en TDD |
| `ProduitJpaRepositoryTest` | 3 | Migrations, recherche sans accents, contrainte SQL |
| `StockJpaAdapterTest` | 4 | Réservation, refus, libération et concurrence |
| `CommandeApiTest` | 4 | API, stock et commande persistée |
| **Total : 7 classes** | **53** | **42 cas hérités + 11 cas d'intégration** |

Les tests d'intégration utilisent un **vrai PostgreSQL 17 jetable** lancé par
Testcontainers. Le conteneur reçoit un port disponible automatiquement ;
`@ServiceConnection` fournit la connexion à Spring. Flyway crée le schéma et
charge les six produits avant les tests. Les bases de ces conteneurs sont
supprimées à l'arrêt : elles ne modifient pas un PostgreSQL déjà installé.
Le premier lancement peut télécharger les dépendances et les images Docker.

Le message `WARNING` ou `WARN` contenant « Serveur SMTP injoignable » dans le test
unitaire de commande est attendu : il prouve que la panne d'e-mail simulée reste
sans effet sur la confirmation. L'erreur SQL `produit_stock_check` est aussi
attendue dans le test qui tente volontairement d'enregistrer un stock négatif.

## 3. Comprendre les couches et les tables

Une requête arrive dans un contrôleur REST. Pour une commande, le contrôleur
construit un `Panier` et appelle le `ServiceCommande` du TP2. Spring lui fournit
les adaptateurs de stock, de paiement, de persistance et de notification.

| Emplacement | Classes et rôle |
| --- | --- |
| `api/` | `ProduitController`, `CommandeController`, demande/réponse JSON et `ErreursApi` |
| `catalogue/` | `ProduitEntity`, `ProduitJpaRepository` et `StockJpaAdapter` |
| `commande/` | Métier du TP2 et ses quatre interfaces |
| `commande/jpa/` | `CommandeEntity`, `LigneCommandeEmbeddable`, dépôt et adaptateur JPA |
| `infra/` | Assemblage Spring, `PaiementSimule` et `NotificationJournal` |
| `panier/`, `fidelite/` | Métier et calculs repris des TP1/TP2 |
| `src/main/resources/db/migration/` | Les trois migrations Flyway |
| `src/test/java/ma/shopflow/TestcontainersConfiguration.java` | PostgreSQL commun aux configurations de test et de démonstration |

| Table PostgreSQL | Mapping et contenu |
| --- | --- |
| `produit` | `ProduitEntity` : référence, nom, prix unitaire et stock positif ou nul |
| `commande` | `CommandeEntity` : client, total, référence de paiement et statut ; `cree_le` est renseigné par le défaut SQL `now()` et n'est pas mappé dans l'entité |
| `ligne_commande` | Collection de `LigneCommandeEmbeddable` rattachée à une commande : produit, quantité et prix au moment de l'achat |

`V1__schema.sql` crée les tables et leurs contraintes ; `V2__catalogue_initial.sql`
charge le catalogue ; `V3__recherche_sans_accents.sql` active `unaccent`.
La recherche combine `lower` et `unaccent` dans PostgreSQL : `ecran` retrouve
« Écran 27 pouces ». Hibernate utilise `ddl-auto=validate` pour contrôler le
schéma construit par Flyway.

Dans `CommandeApiTest`, Spring charge l'application, MockMvc exerce les
contrôleurs et PostgreSQL stocke réellement les commandes. Seule la notification
est remplacée par `@MockitoBean` ; les tests vérifient aussi le contenu des tables
et la restitution du stock après un paiement refusé.

Le paiement reste simulé : les montants jusqu'à 10 000 DH sont acceptés.
La notification est une ligne de journal. Aucun établissement bancaire ni
serveur d'e-mails réel n'est contacté.

## 4. Comprendre et reproduire la survente

Le premier adaptateur faisait « lire, vérifier, modifier ». Avec 20 clients
simultanés pour 8 écrans, plusieurs transactions lisaient le même stock avant
de le réécrire. Le test a observé **20 réservations acceptées au lieu de 8**.
Le commit `848c344` et
[`04-stock-avant-correction.log`](validation/tp3/04-stock-avant-correction.log)
conservent cet état rouge.

Le correctif utilise une seule mise à jour conditionnelle pour la réservation :

```sql
update produit set stock = stock - :quantite
where reference = :reference and stock >= :quantite
```

Le calcul et la condition sont exécutés ensemble par PostgreSQL. Une mise à jour
qui ne modifie aucune ligne provoque le refus de réservation. La libération
utilise aussi `stock = stock + :quantite`, afin de conserver les modifications
concurrentes. Ces deux opérations sont écrites en JPQL dans le dépôt et appelées
depuis l'adaptateur transactionnel.

Le test corrigé confirme **8 réservations acceptées et un stock final de 0** ;
les quatre tests de stock passent. Le commit `634dcaf` conserve le correctif.
La cause et la comparaison avant/après sont
détaillées dans [`ANOMALIES.md`](ANOMALIES.md).

## 5. Lancer l'application et appeler l'API

Pour une démonstration avec PostgreSQL géré par Testcontainers :

```bash
SERVER_PORT=18080 ./tester.sh spring-boot:test-run
```

Avec Java et Maven configurés dans le terminal :

```bash
SERVER_PORT=18080 mvn spring-boot:test-run
```

Cette commande utilise `TestShopflowApplication`, dans les sources de test,
qui ajoute le conteneur PostgreSQL à l'application. Attendre le message
`Started ShopflowApplication`, puis utiliser un deuxième terminal pour les
requêtes. Sur ce poste, le port 8080 est déjà occupé : la démonstration utilise
**18080**. Sur un poste où 8080 est libre, omettre `SERVER_PORT=18080` utilise le
port par défaut ; adapter alors les URL et `@baseUrl` dans `requetes/requetes.http`.

À partir d'une base neuve, exécuter dans cet ordre :

```bash
curl -i 'http://localhost:18080/api/produits?q=ecran'

curl -i -H 'Content-Type: application/json' \
  --data-binary @requetes/commande.json \
  http://localhost:18080/api/commandes

curl -i 'http://localhost:18080/api/produits?q=souris'

curl -i -H 'Content-Type: application/json' \
  --data-binary @requetes/commande-trop-chere.json \
  http://localhost:18080/api/commandes

curl -i 'http://localhost:18080/api/produits?q=portable'
```

Ces requêtes ont été exécutées sur le port 18080. Résultats observés dans
[`08-requetes-http.log`](validation/tp3/08-requetes-http.log) :

| Requête | Résultat observé |
| --- | --- |
| Recherche `ecran` | `200 OK`, produit `ECRA-04` trouvé sans saisir l'accent |
| Clavier + deux souris | `201 Created`, statut `CONFIRMEE`, total de **690 DH** |
| Recherche `souris` après la commande | `200 OK`, stock passé de **40 à 38** |
| Deux ordinateurs portables | `402 Payment Required`, dépassement du plafond bancaire |
| Recherche `portable` après le refus | `200 OK`, stock restitué à **5** |

Le même journal contient cinq autres requêtes réelles : produit en rupture
**409**, produit inconnu **404**, quantité supérieure à dix **422**, demande
invalide **400** et ligne nulle **400**. Les erreurs utilisent le format
`application/problem+json`. La réponse de création contient aussi l'en-tête
`Location` et une référence de paiement.

Chaque nouvelle commande valide réduit le stock. `Ctrl+C` arrête l'application
et son PostgreSQL jetable ; le lancement suivant recrée une base et un catalogue
neufs. Les mêmes requêtes sont disponibles dans `requetes/requetes.http` pour
l'extension REST Client déjà installée sur ce poste.
Le démarrage et l'arrêt gracieux ont été vérifiés dans
[`07-application-locale.log`](validation/tp3/07-application-locale.log) ;
l'application de démonstration a ensuite été arrêtée.
Les conteneurs Testcontainers de la démonstration ont été nettoyés après l'arrêt.

## 6. Preuves et historique

Les résultats Maven originaux sont dans `validation/tp3/` :

| Journal | Résultat observé |
| --- | --- |
| `01-base-tp2.log` | 42 cas hérités au vert avant la migration |
| `02-spring-boot-unitaires.log` | Les mêmes 42 cas au vert après passage à Spring Boot |
| `03-catalogue.log` | 3 tests PostgreSQL au vert |
| `04-stock-avant-correction.log` | 4 tests de stock, 1 échec : 20 ventes au lieu de 8 |
| `05-stock-apres-correction.log` | 4 tests de stock au vert, concurrence corrigée |
| `06-validation-complete.log` | Suite complète : 53 tests, aucun échec ni erreur |
| `07-application-locale.log` | Démarrage sur 18080 et arrêt gracieux, `BUILD SUCCESS` |
| `08-requetes-http.log` | 10 requêtes HTTP réelles : statuts, réponses et stock vérifiés |
| `09-versions-dependances.log` | Versions des bibliothèques effectivement résolues par Maven |

Les tests rouges constituent des preuves de défauts observés ; ils sont conservés
dans l'historique, puis corrigés dans la version courante. Pour consulter les
étapes sans modifier le dossier de travail :

```bash
git tag --list
git log --all --graph --oneline --decorate
git show 848c344:src/main/java/ma/shopflow/catalogue/StockJpaAdapter.java
git diff 848c344..634dcaf -- src/main/java/ma/shopflow/catalogue/
git diff tp2..HEAD --stat
git status --short
```

[`TDD.md`](TDD.md) et `validation/tdd/` conservent les vrais cycles
rouge/vert/refactor du TP2, avec leurs commits d'origine.
Les rapports d'un lancement local se trouvent dans `target/surefire-reports/`.
L'archive de remise `../shopflow-tp3.zip` regroupe le projet dans un dossier
`shopflow/`, avec les sources, requêtes, documents, preuves originales et le
dossier `.git` contenant les tags et l'historique. Les dossiers `target/`,
`.vscode/` et `.idea/` sont exclus.
