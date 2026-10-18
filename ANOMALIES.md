# Fiche d'anomalies - TP1 et TP2 ShopFlow

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

## Validation du TP1

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

## Anomalie 3 (TP2) : stock bloqué après un paiement refusé

**Règle concernée : C5.** Si le paiement est refusé, le stock réservé doit être
libéré avant de lever `PaiementRefuseException`. Aucune commande ne doit être
enregistrée et aucun e-mail ne doit être envoyé.

**Test révélateur :**
`ServiceCommandeTest.unPaiementRefuseLibereLeStock`.

- Préparation : deux souris disponibles à 120 DH, total de 270 DH avec la livraison ;
  la banque renvoie un refus pour « Solde insuffisant ».
- Action : appeler `service.passerCommande(AMINA, panier)`.
- Attendu : réservation, tentative de débit, libération de `SOUR-02` en quantité 2,
  puis exception de paiement ; aucune interaction avec le dépôt ni les notifications.
- Observé avant correction : l'exception attendue est bien levée, mais le stock
  n'est jamais libéré. Mockito signale `Wanted but not invoked: stock.liberer`.

**Cause :** le service levait directement l'exception après le refus de la banque,
sans annuler les réservations. La vérification de l'exception seule ne révélait
pas le bug : il fallait aussi contrôler les appels aux dépendances.

**Correctif :** libérer chaque ligne avant de lever l'exception.

```java
if (!resultat.accepte()) {
    for (LignePanier ligne : panier.lignes()) {
        stock.liberer(ligne.produit().reference(), ligne.quantite());
    }
    throw new PaiementRefuseException(resultat.motifRefus());
}
```

Le test utilise `InOrder` pour vérifier que la libération suit le débit refusé,
et `verifyNoInteractions(commandes, notifications)` pour vérifier l'absence
d'enregistrement et d'e-mail.

## Anomalie 4 (TP2) : une panne d'e-mail fait échouer une commande payée

**Règle concernée : C6.** Une panne du service d'e-mail ne doit pas annuler une
commande dont le paiement est accepté.

**Test révélateur :**
`ServiceCommandeTest.unePanneDeMailNAnnulePasLaCommande`.

- Préparation : stock disponible, paiement accepté (`PAY-789`), dépôt qui renvoie
  la commande enregistrée. `doThrow` programme une
  `IllegalStateException("Serveur SMTP injoignable")` sur `envoyerConfirmation`.
- Action : passer la commande.
- Attendu : une commande `CONFIRMEE` est renvoyée et enregistrée ; le stock reste
  réservé. La panne de notification est journalisée.
- Observé avant correction : l'exception SMTP remonte au client après le débit
  et l'enregistrement. Maven compte une erreur (`Errors: 1`), car le test est
  interrompu avant ses assertions.

**Cause :** l'appel de notification n'était pas protégé. Le client pouvait croire
que la commande avait échoué et recommencer, alors qu'il avait déjà payé.

**Correctif :** limiter le `try/catch` à la notification, intercepter
`RuntimeException` et écrire un avertissement avec `System.Logger`, puis renvoyer
la commande. Les erreurs de paiement et de stockage ne sont pas masquées.

```java
try {
    notifications.envoyerConfirmation(client.email(), commande);
} catch (RuntimeException e) {
    LOG.log(System.Logger.Level.WARNING, "E-mail non envoyé pour la commande {0} : {1}",
            commande.id(), e.getMessage());
}
return commande;
```

## Validation du service de commande (TP2)

Les six tests de commande sont présents à chaque étape, ainsi que les 25 tests
du panier repris du TP1. La fidélité est développée séparément en TDD.

| État du service | Tests exécutés | Échecs | Erreurs | Ignorés |
| --- | ---: | ---: | ---: | ---: |
| Code hérité du TP2 | 31 | 1 | 1 | 0 |
| Après correction de C5 uniquement | 31 | 0 | 1 | 0 |
| Après correction de C5 et C6 | 31 | 0 | 0 | 0 |

Le message `WARNING` contenant « Serveur SMTP injoignable » lors du test de C6
est attendu : il confirme que la panne a été simulée et journalisée.

Commande de reproduction : `./tester.sh -Dtest=ServiceCommandeTest test`.
La version finale avec les 11 cas de fidélité doit afficher **42 tests au vert**.
