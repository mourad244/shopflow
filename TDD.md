# Journal TDD du programme de fidélité

Les cinq cycles de l'étape 6 du TP2 ont été exécutés le 18 octobre 2026,
dans l'ordre rouge, vert, puis refactor lorsqu'une restructuration est demandée.
Chaque état rouge a été exécuté et commité avant l'écriture de son implémentation.
Chaque état vert et chaque refactor ont été exécutés avec succès avant leur commit.
Les montants utilisent `BigDecimal` construit à partir de chaînes de caractères.

## Point de départ et commande

Le point de départ est le commit `cd78471`, qui ajoute Mockito et AssertJ au TP1.
La suite complète du TP1 réellement présent dans ce dépôt contient **25 exécutions** :
elles sont toutes vertes avant le premier cycle ([00-baseline.log](validation/tdd/00-baseline.log)). Ce nombre
explique l'écart avec les totaux globaux illustrés dans le PDF : les résultats
ci-dessous comptent les tests de fidélité, indépendamment des tests du service de
commande développés dans les autres étapes du TP2.

Commande utilisée pour observer chaque rouge, vert et refactor :

```bash
./tester.sh -B -ntp -Dtest=CalculateurFideliteTest test
```

Le script sélectionne sur ce poste le JDK 21 et Maven 3.9 fournis par IntelliJ.
`-Dtest` exécute seulement le calculateur de fidélité : les cibles successives
**1, 6, 7, 8 et 11** sont des nombres d'exécutions, et non des nombres de méthodes.
Les deux méthodes paramétrées représentent respectivement cinq et trois exécutions.

## Résultats observés

| Cycle et phase | Résultat réellement observé | Commit après exécution | Journal |
| --- | --- | --- | --- |
| 1 rouge | Échec de compilation : `CalculateurFidelite` n'existe pas encore ; aucune exécution de test. | `ff611e6` | [01-rouge.log](validation/tdd/01-rouge.log) |
| 1 vert | `return 10;` suffit au premier exemple ; 1 exécution, 0 échec. | `5055c1a` | [01-vert.log](validation/tdd/01-vert.log) |
| 2 rouge | Les cinq lignes CSV ajoutées échouent ; 6 exécutions, 5 échecs, 0 erreur. | `44aa172` | [02-rouge.log](validation/tdd/02-rouge.log) |
| 2 vert | Division entière par 10 avec `RoundingMode.DOWN` ; 6 exécutions, 0 échec. | `b72de21` | [02-vert.log](validation/tdd/02-vert.log) |
| 3 rouge | Échec de compilation : `NiveauFidelite` et la surcharge à deux arguments ne sont pas encore disponibles ; aucune exécution. Le diagnostic de compilation nomme `NiveauFidelite`. | `7dd3675` | [03-rouge.log](validation/tdd/03-rouge.log) |
| 3 vert | Énumération STANDARD/GOLD et calcul volontairement dupliqué avec un `if` ; 7 exécutions, 0 échec. | `adb68f8` | [03-vert.log](validation/tdd/03-vert.log) |
| 3 refactor | Multiplicateur porté par l'énumération, délégation à STANDARD et méthode de calcul de base ; 7 exécutions, 0 échec. | `fcd11b8` | [03-refactor.log](validation/tdd/03-refactor.log) |
| 4 rouge | Le montant négatif ne déclenche aucune exception : `Expecting code to raise a throwable.` ; 8 exécutions, 1 échec, 0 erreur. | `a5f78c0` | [04-rouge.log](validation/tdd/04-rouge.log) |
| 4 vert | Garde sur `montant.signum() < 0` et message exact ; 8 exécutions, 0 échec. | `5a296c5` | [04-vert.log](validation/tdd/04-vert.log) |
| 5 rouge | 6000 DH STANDARD et 3000 DH GOLD donnent 600 au lieu de 500 ; 11 exécutions, 2 échecs, 0 erreur. | `5dbcc9f` | [05-rouge.log](validation/tdd/05-rouge.log) |
| 5 vert | `Math.min` applique le plafond après le multiplicateur ; 11 exécutions, 0 échec. | `35b587b` | [05-vert.log](validation/tdd/05-vert.log) |
| 5 refactor | La valeur 500 est nommée `PLAFOND_PAR_COMMANDE` ; 11 exécutions, 0 échec. | `292df5c` | [05-refactor.log](validation/tdd/05-refactor.log) |

Les journaux d'exécution sont conservés dans le dossier `validation/tdd/`
du projet et de son archive. Ils contiennent la sortie Maven originale ;
les journaux rouges se terminent volontairement par `BUILD FAILURE`.
Les commits rouges peuvent aussi être réexécutés dans un checkout propre pour
observer les échecs avant de passer au commit vert suivant.

## Ce que chaque rouge impose

1. **Premier exemple :** 100,00 DH doivent rapporter 10 points. La classe et sa
   méthode `points(BigDecimal)` sont conçues à partir du test, puis une valeur
   constante prouve que le test peut passer.
2. **Triangulation :** 0,00 et 9,99 DH attendent 0 ; 10,00 attend 1 ; 99,99 attend 9 ;
   250,00 attend 25. Ces cinq échecs éliminent la valeur 10 codée en dur et imposent
   les tranches entières de 10 DH.
3. **Niveau GOLD :** 250,00 DH en GOLD doivent rapporter 50 points. Une première
   version simple avec duplication passe avant le déplacement du multiplicateur
   dans `NiveauFidelite`. Le refactor conserve les sept exécutions au vert.
4. **Montant négatif :** -20,00 DH doivent lever `IllegalArgumentException` avec
   exactement `Le montant ne peut pas être négatif`. Le rouge prouve l'absence
   initiale de validation ; la garde satisfait ensuite le type et le message.
5. **Plafond :** 6000,00 DH en STANDARD attendent 500 ; 2400,00 DH en GOLD attendent
   480 ; 3000,00 DH en GOLD attendent 500. Le cas à 480 passe déjà au rouge parce
   qu'il reste sous le plafond. Le dernier cas protège l'ordre du calcul :
   `min(300, 500) * 2` donnerait à tort 600, alors que `min(300 * 2, 500)` donne 500.

## Vérification finale

Après le dernier refactor, `./tester.sh -B -ntp test` réussit dans le clone isolé :
**36 exécutions, 0 échec, 0 erreur, 0 test ignoré**, soit les 25 exécutions du TP1
et les 11 exécutions du calculateur ([06-complet.log](validation/tdd/06-complet.log)). Ce total a été mesuré avant
l'intégration avec les autres étapes du TP2.

Le périmètre reste celui des exemples obligatoires du sujet : le test de fidélité
contient trois méthodes `@Test` et deux méthodes `@ParameterizedTest`, pour onze
exécutions au total.

## Vérification après intégration avec les commandes

La branche `tp2-fidelite-tdd` a été fusionnée dans `main` en conservant les commits
du tableau. Dans le projet final `shopflow-tp2`, la commande
`./tester.sh -B -ntp test` a ensuite été exécutée : **42 tests, 0 échec, 0 erreur,
0 test ignoré**. Ce total comprend les 25 cas du TP1, les 6 cas du service de
commande et les 11 cas de fidélité. Les résultats intermédiaires à 36 ci-dessus
concernent uniquement le clone isolé avant cette intégration.
