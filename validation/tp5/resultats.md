# Résultats du TP5

## Exécution Maven et couverture

| Commande et état du projet | Tests unitaires | Tests Failsafe | Lignes | Branches | Résultat |
| --- | ---: | ---: | ---: | ---: | --- |
| `clean verify` avant les cinq nouveaux cas (`a6ced9d`) | 42 | 26 | 229/244 (93,85 %) | 48/54 (88,89 %) | Tests réussis. Build bloqué par les seuils de 95 % et 90 %. |
| `clean test` après ajout des cas | 47 | — | — | — | Succès. |
| `clean verify` avec les seuils définitifs | 47 | 26 | 233/244 (95,49 %) | 52/54 (96,30 %) | 73 tests réussis. Seuils de 90 % de lignes et 85 % de branches respectés. |

Le premier rapport montrait la méthode `Panier.retirer` sans couverture. Deux tests vérifient le retrait et le cas d’un produit absent. Trois cas paramétrés vérifient le refus d’un prix absent, nul ou négatif. Les 26 tests Failsafe comprennent 15 tests d’intégration et 11 scénarios d’acceptation.

## Mutation

PIT a couvert 101/101 lignes du périmètre métier. Sur les 49 mutants générés, 48 sont tués (98 %) et un survit. En supprimant l’appel à `System.Logger.log` dans [ServiceCommande](../../src/main/java/ma/shopflow/commande/ServiceCommande.java), le mutant conserve le résultat attendu par le test C6. La commande reste confirmée malgré la panne d’e-mail, mais la panne ne laisse plus de trace. Ce survivant est accepté pour le TP5 et sa conséquence pour le diagnostic est consignée dans la [fiche d’anomalies](../../ANOMALIES.md).

## SonarQube Community Build 26.9.0

| Analyse | Quality Gate | Signalements ouverts | Couverture SonarQube | Lignes dupliquées |
| --- | --- | ---: | ---: | ---: |
| Avant correction | Passed | 1 (`java:S2187` sur l’horloge des scénarios) | 95,0 % | 0,0 % |
| Après correction | Passed | 0 | 95,6 % | 0,0 % |

La classe auxiliaire `HorlogeDeTest` a pris le nom `HorlogeReglable`. SonarQube importe le rapport XML de JaCoCo et exclut désormais `ShopflowApplication.java` du calcul de couverture, comme JaCoCo. Les analyses ont toutes deux attendu la réponse du serveur. La dernière analyse relève 95,5 % de lignes couvertes et 96,3 % de branches couvertes. La [capture du tableau de bord](sonarqube.png) et les [extraits d’exécution](execution.log) permettent de vérifier ces résultats.
