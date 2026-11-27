# Examen — Réseau de neurones, Design Patterns et AOP

Implémentation Java de l'énoncé transmis en deux captures : réseau feed-forward, apprentissage réel, **Strategy, Adapter, State, Observer**, complétés par **Builder** et **Simple Factory**.

## Livrables

- [Rapport PDF](dist/RAPPORT.pdf) : conception, diagrammes UML, code, tests et limites.
- [Rapport Markdown](RAPPORT.md).
- [Archive ZIP à remettre](dist/Examen-Reseau-Neurones-AOP.zip).
- Diagrammes : [docs/diagrammes.md](docs/diagrammes.md) et PNG dans `docs/`.
- Modèle indépendant : `network-core/`.
- Aspects de journalisation et sécurité : `network-aop/`.

## Compilation et tests

JDK **21 ou supérieur** et Internet au premier lancement. Le Maven Wrapper télécharge Maven 3.9.9.

```powershell
.\mvnw.cmd clean verify
```

Sous Linux/macOS : `sh mvnw clean verify`.

Les tests couvrent les calculs, la Jacobienne Softmax, l'apprentissage, les états, les notifications, les poids bornés, les erreurs numériques, les droits et la journalisation.

## Démonstrations

Installer d'abord les deux modules dans le dépôt Maven local :

```powershell
.\mvnw.cmd install
```

Modèle sans aspects :

```powershell
.\mvnw.cmd -pl network-core exec:java "-Dexec.mainClass=fr.enset.nn.demo.DemoModele"
```

Scénario avec les véritables classes tissées par AspectJ :

```powershell
.\mvnw.cmd -pl network-aop exec:java "-Dexec.mainClass=fr.enset.nn.demo.DemoSecurisee"
```

Cette seconde démonstration refuse un utilisateur anonyme, autorise l'administrateur, puis refuse la prédiction au lecteur. Comptes **fictifs** `admin` / `lecteur`, mot de passe local `Demo-2027!`.

La démonstration sécurisée charge `logging.properties` avant son `main`. Les traces d'entrée/sortie, durée et échec sont au niveau JUL **FINE** dans `neural-network-audit-*.log*`, dans le répertoire courant. Les autres modes d'exécution doivent aussi activer ce niveau pour enregistrer l'audit détaillé.

Le module `network-core` n'applique pas la sécurité : ses annotations sont inertes. Pour une exécution sécurisée, utiliser `network-aop`, et non le JAR non tissé.

## Hypothèses de l'énoncé

- Sigmoïde corrigée : `1 / (1 + exp(-x))` ; le signe moins imprimé au dénominateur est une coquille.
- États retenus : **Construction → Training → Trained**. `fit` est un alias de `train`, pas un faux passage direct à un état d'exécution non défini.
- `predict` n'est possible qu'en état `Trained`.
- Chaque neurone possède une stratégie ; Softmax reçoit le vecteur de sa couche et doit couvrir toute cette couche.
- Poids dans **[0,1]**, y compris après apprentissage ; projection après chaque mise à jour.
- Réseau dense acyclique, connexions entre couches consécutives, sans biais ni entraînement récurrent.

## Résultats numériques de la démonstration

- Architecture **3–5–2** : **25 connexions**.
- Sortie Softmax : environ `[0.775543, 0.224457]`.
- Apprentissage de `y = 0.25*x1 + 0.75*x2` : perte finale de l'ordre de `1e-20` sur les exemples.
- Prédiction pour `[0.4, 0.8]` : environ **0.7**.

Il ne s'agit pas d'un apprentissage XOR : les contraintes de poids positifs et l'absence de biais limitent les fonctions représentables.

## Régénérer le PDF et le ZIP

```powershell
python -m pip install -r tools/requirements-report.txt
python tools/build_deliverables.py
```

Faire d'abord `.\mvnw.cmd clean verify` pour inclure des résultats de tests à jour. Le script exclut `.git`, `target`, les journaux et les archives ZIP de l'archive finale. Il ajoute une copie des résultats JUnit dans `docs/resultats-tests.json`.

## Publication

- Ce projet : [Neural-Network-design-patterns](https://github.com/CHAIMAEMOUHSSINE/Neural-Network-design-patterns).
- Activité AOP séparée : [Aspect-oriented-Programming-demo-](https://github.com/CHAIMAEMOUHSSINE/Aspect-oriented-Programming-demo-).

Aucun ancien projet de dessins n'a été remplacé. Les PDF et ZIP sont disponibles dans `dist/`.

## Limites

Projet pédagogique synchrone, sans interface graphique, sans base de données ni session réseau. L'authentification est locale en mémoire ; l'inscription est du bootstrap de confiance, sans durcissement de production.

Les droits protègent les méthodes du réseau, pas l'exécution de code Java hostile détenant déjà des références. L'inscription des observateurs n'est pas une garantie de concurrence. Une exception d'observateur de type `RuntimeException` est isolée et comptabilisée ; une erreur fatale n'est pas promise comme récupérable.

Les classes d'activation peuvent être réutilisées hors réseau ; le réseau valide les dimensions et les valeurs avant de les appeler. La modification d'une activation ou d'un poids invalide un réseau entraîné et exige un nouvel apprentissage.
