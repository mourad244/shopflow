# Fiche d'anomalies - TP1 ShopFlow

## Anomalie 1 : livraison facturée à exactement 500 DH

**Règle concernée : R5.** La livraison coûte 30 DH, mais elle est offerte dès que
le sous-total atteint 500 DH. Un panier vide ne paie pas de livraison.

**Test révélateur :**
`PanierTest.FraisDeLivraison.laLivraisonEstOfferteAPartirDe500DH`,
jeu de données `500.00, 0.00` (troisième cas du `@CsvSource`).

- Préparation : ajouter une unité d'un produit dont le prix est 500.00 DH.
- Action : appeler `panier.fraisLivraison()`.
- Attendu : **0.00 DH**.
- Observé avant correction : **30.00 DH**.

**Cause :** dans `Panier.fraisLivraison()`, le résultat de `compareTo` était
comparé à zéro avec `> 0`. Cette condition signifie « strictement supérieur à
500 DH » et exclut le seuil exact, pour lequel `compareTo` renvoie zéro.

**Correctif :** remplacer `> 0` par `>= 0`.

```java
return sousTotal().compareTo(SEUIL_LIVRAISON_OFFERTE) >= 0
        ? BigDecimal.ZERO.setScale(2)
        : FRAIS_LIVRAISON;
```

Les cas 499.99, 500.00 et 500.01 DH encadrent la limite. Le test est conservé pour
détecter une réintroduction du bug.

## Anomalie 2 : remise appliquée aux frais de livraison

**Règles concernées : R6 et R7.** La remise porte uniquement sur le sous-total,
jamais sur la livraison. Le total vaut `sous-total - remise + livraison`.

**Test révélateur :**
`PanierTest.RemiseEtTotal.laRemiseNePortePasSurLaLivraison`.

- Préparation : ajouter une souris à 120.00 DH, puis appliquer le code
  `BIENVENUE10` (10 %).
- Action : calculer la remise, les frais de livraison et le total.
- Attendu : remise **12.00 DH**, livraison **30.00 DH**, total **138.00 DH**.
- Observé avant correction : remise 12.00 DH et livraison 30.00 DH correctes,
  mais total **135.00 DH**.

**Cause :** la méthode `total()` additionnait d'abord les produits et la
livraison, puis retirait 10 % de ce montant :
`(120 + 30) - 10 % de 150 = 135 DH`. Elle accordait donc aussi une réduction de
3 DH sur la livraison. La méthode `remise()` était déjà correcte, mais `total()`
ne l'utilisait pas.

**Correctif :** utiliser directement les trois calculs métier dans l'ordre de R7.

```java
public BigDecimal total() {
    return sousTotal().subtract(remise()).add(fraisLivraison());
}
```

Le seuil de livraison reste calculé sur le sous-total avant remise. Le cas d'un
panier à 570.00 DH avec 10 % de remise donne ainsi 513.00 DH de total et zéro
frais de livraison. Ce cas seul ne révélait pas le bug, puisque la livraison
y est déjà gratuite.

## Validation

Les mêmes 25 cas sont exécutés à chaque étape, sous Java 21 avec Maven 3.9 :

| État du code | Tests exécutés | Échecs | Erreurs | Ignorés |
| --- | ---: | ---: | ---: | ---: |
| Code hérité du sujet | 25 | 2 | 0 | 0 |
| Après correction de R5 uniquement | 25 | 1 | 0 | 0 |
| Après correction de R5 et du total | 25 | 0 | 0 | 0 |

Commande de reproduction sur ce poste : `./tester.sh`.
Avec Java 21 et Maven installés dans le `PATH` : `mvn test`.

L'échec pédagogique de `assertEquals` entre `690` et `690.00` est une erreur de
comparaison dans le test, et non une troisième anomalie métier. Les tests finaux
comparent les montants par leur valeur avec `BigDecimal.compareTo`.
