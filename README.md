# TP1 - Tests unitaires avec JUnit 5

Solution du panier **ShopFlow**, suivant le sujet du Pr. Youssef Gahi.
Le nom du dossier local est `tes_unitaire_master`, mais le projet Maven s'appelle
`shopflow` et le paquet Java est `ma.shopflow.panier`.

## 1. Lancer les tests sur ton poste

Ouvre ce dossier dans VS Code ou IntelliJ, puis ouvre un terminal à sa racine :

```bash
./tester.sh
```

Le script utilise le JDK 21 et Maven déjà fournis par IntelliJ sur ce poste.
Il ne modifie pas les paramètres du système. Le Java par défaut du terminal est
actuellement en version 17 et la commande `mvn` n'est pas dans le `PATH` : le script
permet donc de lancer ce projet directement. Si tu as déjà défini `JAVA_HOME`,
vérifie qu'il désigne un JDK 21 ou ultérieur.

Sur un poste disposant de Java 21 et Maven 3.9 dans le `PATH` :

```bash
java -version
mvn -version
mvn test
```

La ligne Java de `mvn -version` doit indiquer 21 ou une version ultérieure.
Au premier lancement, Maven télécharge JUnit et les plugins : une connexion
Internet est nécessaire. Le résultat attendu est :

```text
Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Tu peux aussi lancer uniquement les tests des codes promo :

```bash
./tester.sh -Dtest=CodePromoTest test
```

## 2. Comprendre les fichiers

```text
pom.xml                        Configuration Maven, Java 21 et JUnit 5
src/main/java/ma/shopflow/panier/
  Produit.java                 Référence, nom et prix d'un produit
  LignePanier.java             Produit + quantité, calcul d'une ligne
  CodePromo.java               Pourcentage autorisé entre 1 et 50
  QuantiteMaximaleDepasseeException.java
  Panier.java                  Ajout, sous-total, livraison, remise et total
src/test/java/ma/shopflow/panier/
  PanierTest.java               18 cas exécutés
  CodePromoTest.java             7 cas exécutés
ANOMALIES.md                   Les deux bugs et leurs corrections
tester.sh                      Lance Maven avec l'outillage disponible
```

Le `pom.xml` reprend les versions du sujet : JUnit 5.14.4 et Surefire 3.5.6.
Le plugin de compilation est également fixé à 3.14.1 pour utiliser explicitement
`maven.compiler.release=21`, indépendamment des valeurs par défaut de Maven.

## 3. Démarche pour résoudre et expliquer le TP

1. **Lire les règles R1 à R8.** Les résultats attendus viennent du cahier des règles.
2. **Écrire les cinq classes métier.** Dans le TP, on commence avec le code hérité,
   qui contient deux défauts. La version livrée ici contient leurs correctifs.
3. **Écrire les tests.** Chaque test prépare ses données, appelle la méthode et
   vérifie le résultat : Arrange / Act / Assert.
4. **Organiser les cas par règle.** Les classes `@Nested` regroupent l'ajout,
   le sous-total, la livraison, la remise et le code promo sur le panier.
5. **Tester les limites.** Vérifier 10 unités acceptées, un cumul de 11 refusé,
   ainsi que 499.99, 500.00 et 500.01 DH pour la livraison.
6. **Observer les échecs, corriger et relancer.** Les tests doivent détecter les
   deux anomalies avant la correction, puis rester dans le projet pour éviter
   leur réapparition.
7. **Documenter et versionner.** Décrire chaque anomalie et conserver l'état final
   dans Git avec le tag `tp1`.

Exemple du schéma AAA, issu du test du total sans promotion :

```java
@Test
void sansCodePromoLeTotalInclutLaLivraison() {
    // Arrange : un panier vide est créé par @BeforeEach.
    panier.ajouter(SOURIS, 1);

    // Act : calculer le total.
    BigDecimal resultat = panier.total();

    // Assert : 120 DH de produit + 30 DH de livraison.
    assertMontant("150.00", resultat);
}
```

## 4. Les annotations et assertions à connaître

| Élément | Rôle dans ce TP |
| --- | --- |
| `@Test` | Déclare un test simple |
| `@BeforeEach` | Crée un nouveau panier avant chaque cas, y compris les cas paramétrés |
| `@Nested` | Regroupe les tests par règle métier |
| `@DisplayName` | Affiche un nom lisible |
| `assertThrows` | Vérifie une exception, puis on vérifie aussi l'état du panier |
| `assertDoesNotThrow` | Vérifie qu'une valeur autorisée est acceptée |
| `assertAll` | Vérifie plusieurs résultats, même si une assertion échoue |
| `@ParameterizedTest` | Exécute une méthode avec plusieurs jeux de données |
| `@CsvSource` | Fournit des couples sous-total / livraison attendue |
| `@ValueSource` | Fournit les pourcentages acceptés ou refusés |

Attention aux montants : `new BigDecimal("690").equals(new BigDecimal("690.00"))`
est faux, car `equals` tient compte du nombre de décimales.
Le helper `assertMontant` utilise `compareTo`, qui compare la valeur numérique.
Construis les montants avec des chaînes (`"19.99"`), pour éviter les imprécisions
d'une construction à partir d'un `double`.

## 5. Pourquoi exactement 25 tests ?

JUnit compte chaque jeu de données d'un test paramétré comme une exécution.

| Groupe | Cas exécutés |
| --- | ---: |
| R1 à R3 : ajout de produits | 5 |
| R4 : sous-total | 2 |
| R5 : livraison (panier vide + 5 montants) | 6 |
| R6 et R7 : remise et total | 4 |
| R8 : code promo sur panier vide | 1 |
| Pourcentages acceptés : 1, 25, 50 | 3 |
| Pourcentages refusés : -5, 0, 51, 100 | 4 |
| **Total** | **25** |

Il y a donc 16 méthodes de test, mais 25 cas exécutés.

## 6. Les deux corrections à savoir expliquer

**Livraison :** à 500 DH exactement, `compareTo` renvoie zéro. La condition doit
être `>= 0` pour respecter « offerte dès 500 DH ».

```java
return sousTotal().compareTo(SEUIL_LIVRAISON_OFFERTE) >= 0
        ? BigDecimal.ZERO.setScale(2)
        : FRAIS_LIVRAISON;
```

**Total :** la remise s'applique uniquement aux produits. Pour une souris de
120 DH avec une promotion de 10 %, on paie `120 - 12 + 30 = 138 DH`.
L'ancien calcul donnait `(120 + 30) * 0.90 = 135 DH`.

```java
public BigDecimal total() {
    return sousTotal().subtract(remise()).add(fraisLivraison());
}
```

Les détails et les tests révélateurs figurent dans `ANOMALIES.md`.

## 7. Livrable et vérification Git

Le dépôt final doit contenir les sources, les tests et `ANOMALIES.md`, avec le tag
`tp1`. Pour consulter cet état :

```bash
git log -1 --oneline
git tag --list
git status --short
```

L'archive à déposer doit inclure le dossier `.git`, pour conserver le dépôt et
son tag, et exclure `target/`, qui ne contient que des fichiers générés.
Les résultats détaillés d'un lancement sont disponibles localement dans
`target/surefire-reports/`.

Les tests du retrait, d'un second code promo et les tags JUnit sont des
extensions facultatives proposées à la fin du sujet.
