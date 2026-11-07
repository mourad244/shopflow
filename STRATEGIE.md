# Stratégie de test de ShopFlow

Une commande confirmée doit débiter le bon montant, réserver le stock une seule fois et respecter les dates annoncées au client. Les tests vérifient ces règles à trois niveaux.

| Niveau | Risques couverts et outils | Quand |
| --- | --- | --- |
| Unitaire · 47 cas | JUnit et AssertJ vérifient le panier, les remises et les points de fidélité. Mockito vérifie les interactions du service de commande avec le stock, le paiement et la notification. | Pendant le développement avec `mvn test`, puis dans le premier job GitHub Actions. |
| Intégration · 15 cas | PostgreSQL lancé par Testcontainers exerce les migrations, la recherche sans accent et les réservations simultanées. MockMvc et REST Assured vérifient l’API, les erreurs HTTP et la persistance. | Avant un push avec `mvn verify`, puis dans le deuxième job de CI si le premier réussit. |
| Acceptation · 11 scénarios | Cucumber exécute les exemples métier en français contre l’API avec REST Assured. Une horloge réglable permet de tester la veille, le jour et le lendemain d’une expiration. | Avec les tests d’intégration, sur les push vers `main` et les pull requests. |

## Décision de livraison

- Un seul test en échec bloque la validation Maven. JaCoCo mesure les deux étages Maven ensemble. Le build échoue sous **90 % de lignes** ou **85 % de branches** couvertes.
- L’équipe exige le **Quality Gate SonarQube** avant la livraison. L’analyse est exécutée sur le serveur local avec l’option d’attente du résultat. GitHub Actions valide les tests et la couverture et publie les rapports JaCoCo et Cucumber.
- PIT évalue à la demande la capacité des tests unitaires à détecter des changements dans les paquets panier, fidélité et commande. Chaque mutant survivant fait l’objet d’une décision documentée.

## Ce que chaque niveau a révélé

| Étape | Défaut révélé |
| --- | --- |
| TP1 | Livraison facturée à 500 DH et remise appliquée aux frais de port. |
| TP2 | Stock conservé après un paiement refusé et commande annulée lors d’une panne d’e-mail. |
| TP3 | Vingt réservations pouvaient aboutir pour huit écrans en stock. |
| TP4 | Le code BLACKFRIDAY était refusé le dernier jour de validité. |
| TP5 | PIT a supprimé le journal de la panne d’e-mail sans faire échouer C6. SonarQube a signalé que le nom de l’horloge la faisait passer pour une classe de test vide. |
