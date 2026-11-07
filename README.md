# ShopFlow · TP4 — Tests d’acceptation

ShopFlow accepte un code promo facultatif sur `POST /api/commandes`. Le client peut utiliser le code jusqu’à sa date d’expiration incluse. La remise s’applique au sous-total des produits. Un code inconnu ou expiré entraîne une réponse HTTP 422 avant la réservation du stock et le paiement.

## Exécuter les tests

Avec Docker démarré, Java 21 et Maven, lancer les commandes depuis la racine du projet. Testcontainers démarre PostgreSQL et Spring Boot ouvre un port HTTP libre.

| Commande | Tests exécutés |
| --- | --- |
| `./tester.sh -Dtest=ProduitApiTest test` | Catalogue, recherche sans accent, commande confirmée et produit inconnu avec REST Assured |
| `./tester.sh -Dtest=AcceptationTest test` | Les 11 scénarios Gherkin, soit 6 pour la commande et 5 pour les codes promo |
| `./tester.sh clean test` | La suite complète, y compris les tests des TP précédents |

`mvn` peut remplacer `./tester.sh`. Sur le poste de travail utilisé pour ce projet, le script retrouve aussi Java et Maven dans l’installation IntelliJ.

Le [dernier lancement complet](validation/tp4/suite-complete.log) a exécuté **68 tests** avec zéro échec, erreur ou test ignoré. Les 53 tests du TP3 sont complétés par 4 tests REST Assured et 11 scénarios Cucumber.

## Lire les scénarios et le rapport

Les exemples métier sont dans [commande.feature](src/test/resources/fonctionnalites/commande.feature) et [code_promo.feature](src/test/resources/fonctionnalites/code_promo.feature). Pour deux souris à 120 DH avec `BIENVENUE10`, le total attendu est de **246 DH** (240 DH − 24 DH + 30 DH de livraison). Les exemples `BLACKFRIDAY` fixent l’horloge au 29 novembre, au 30 novembre et au 1ᵉʳ décembre 2026 pour vérifier les deux côtés de la date limite.

Le [rapport HTML de la remise](rapport.html) présente chaque étape en français. Un lancement de `AcceptationTest` régénère `target/cucumber/rapport.html` et `target/cucumber/rapport.json`.

## Versions précédentes

Les tags `tp1`, `tp2`, `tp3` et `tp4` conservent la progression du projet. La commande `git show tp3:README.md` affiche les instructions du TP3. La [fiche d’anomalies](ANOMALIES.md) décrit les défauts observés depuis le TP1.
