# ShopFlow · TP5

ShopFlow vérifie les règles de commande, l’accès à PostgreSQL et les scénarios métier avec Maven et Java 21. La [fiche de stratégie](STRATEGIE.md) décrit les tests et les conditions de livraison. Le tag `tp5` complète l’historique des TP1 à TP4.

## Exécuter les tests et les mesures

| Commande | Résultat |
| --- | --- |
| `./tester.sh clean test` | Surefire lance les 47 tests unitaires. Docker peut être arrêté. |
| `./tester.sh clean verify` | Surefire puis Failsafe exécutent 73 tests au total. Docker doit être démarré pour PostgreSQL. JaCoCo applique les seuils et génère `target/site/jacoco/index.html`. |
| `./tester.sh test-compile org.pitest:pitest-maven:mutationCoverage` | PIT modifie le code du domaine et lance les tests unitaires concernés. Ouvrir `target/pit-reports/index.html`. |

`mvn` peut remplacer `./tester.sh`. Les cinq classes d’intégration et d’acceptation se terminent par `IT`. Failsafe génère le rapport Cucumber dans `target/cucumber/rapport.html`.

## Analyser avec SonarQube

1. Démarrer le service avec `docker compose up -d`. Le projet utilise le port **19000** sur la boucle locale, le port 9000 étant déjà occupé sur le poste.
2. Ouvrir [le serveur SonarQube](http://localhost:19000), terminer la configuration administrateur et générer un jeton d’analyse pour le projet `shopflow`.
3. Depuis Bash, saisir le jeton dans une invite masquée, lancer l’analyse puis effacer la variable.

```bash
read -rs -p 'Jeton SonarQube : ' SONAR_TOKEN && echo
SONAR_TOKEN="$SONAR_TOKEN" ./tester.sh clean verify sonar:sonar -Dsonar.qualitygate.wait=true
unset SONAR_TOKEN
```

Le [tableau de bord local](http://localhost:19000/dashboard?id=shopflow) affiche le Quality Gate et les mesures. Une [capture du tableau de bord](validation/tp5/sonarqube.png) est incluse pour la remise.

## Remise

- [Pipeline GitHub Actions](https://github.com/mourad244/shopflow/actions/workflows/ci.yml). Le premier job teste les règles métier. Le second exécute la suite complète et publie les artefacts `couverture-jacoco` et `rapport-cucumber`.
- [Rapports du tag tp5](https://github.com/mourad244/shopflow/releases/tag/tp5). Télécharger et extraire l’archive pour consulter les rapports HTML JaCoCo, PIT et Cucumber.
- [Résultats des vérifications](validation/tp5/resultats.md) et [fiche d’anomalies](ANOMALIES.md).
