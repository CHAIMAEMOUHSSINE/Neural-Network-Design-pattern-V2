# Examen V2 — Réseau de neurones, Design Patterns et AOP

Solution Java du PDF **Examen Design Pattern V2** : réseau feed-forward avec **biais entraînables**, apprentissage réel, **Strategy, Adapter, State, Observer, Decorator**, complétés par **Builder** et **Simple Factory**.

Cette version utilise le package **`chaimae.net`** et le dépôt dédié [Neural-Network-Design-pattern-V2](https://github.com/CHAIMAEMOUHSSINE/Neural-Network-Design-pattern-V2). La version précédente reste conservée dans son dépôt d'origine.

## Livrables

- [Rapport PDF](dist/RAPPORT.pdf) : conception, diagrammes UML, code, tests et limites.
- [Rapport Markdown](RAPPORT.md).
- [Archive ZIP V2 à remettre](dist/Examen-Reseau-Neurones-AOP-V2.zip).
- Diagrammes : [docs/diagrammes.md](docs/diagrammes.md) et PNG dans `docs/`.
- Modèle indépendant : `network-core/`.
- Aspects de journalisation et sécurité : `network-aop/`.

## Compilation et tests

JDK **21 ou supérieur** et Internet au premier lancement. Le Maven Wrapper télécharge Maven 3.9.9.

```powershell
.\mvnw.cmd clean verify
```

Sous Linux/macOS : `sh mvnw clean verify`.

**53 tests passent** : 8 activations, 16 réseau, 9 biais, 10 décorateurs et 10 aspects. Ils couvrent aussi les gradients des décorateurs, les biais, la reprise sur panne et le refus des nouvelles mutations sans le rôle CONFIGURE.

## Correspondance avec les consignes a–g

- a : Strategy — `ActivationFunction`, Identity, Sigmoid et Softmax.
- b : Adapter — `LegacyActivationAdapter` adapte l'interface scalaire `LegacyActivation`.
- c : State — Construction, Training et Trained ; `predict` après apprentissage seulement.
- d : Observer — `NetworkObserver`, `Dashboard`, historique et statistiques d'états.
- e : Decorator — `LayerComponent`, `LayerDecorator`, statistiques et mise à l'échelle.
- f : `LoggingAspect` — méthodes applicatives, y compris privées et décorateurs.
- g : `SecurityAspect` — authentification et contrôle des rôles sur le réseau.

Les diagrammes, extraits Java et tests commentés sont inclus dans le rapport. Les sources compilables complètes sont dans le ZIP.

## Enrichir une couche dynamiquement

```java
network.decorateLayer(1, layer -> new ScalingLayerDecorator(layer, 1.1));
network.decorateLayer(1, StatisticsLayerDecorator::new);
// Après un nouvel entraînement :
var stats = (StatisticsLayerDecorator) network.layers().get(1);
System.out.println(stats.statistics());
network.undecorateLayer(1); // retire l'enveloppe extérieure et invalide Trained
```

Toutes les couches, y compris l'entrée, peuvent être décorées. Les décorateurs ne dupliquent pas les neurones. La mise à l'échelle adapte aussi les dérivées ; elle est refusée sur Softmax si son facteur diffère de 1. Toute reconfiguration demande un nouvel entraînement ; elle est interdite pendant Training.

## Démonstrations

Installer d'abord les deux modules dans le dépôt Maven local :

```powershell
.\mvnw.cmd install
```

Modèle sans aspects :

```powershell
.\mvnw.cmd -pl network-core exec:java "-Dexec.mainClass=chaimae.net.demo.DemoModele"
```

Scénario avec les véritables classes tissées par AspectJ :

```powershell
.\mvnw.cmd -pl network-aop exec:java "-Dexec.mainClass=chaimae.net.demo.DemoSecurisee"
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
- Réseau dense acyclique, connexions entre couches consécutives, sans entraînement récurrent.
- Biais initialisés à zéro, réels finis et entraînables hors couche d'entrée. L'entrée conserve des biais nuls et une activation de base Identity.
- Les statistiques des couches comptent les évaluations, pas les epochs ; elles ne sont pas annulées lors d'une restauration des paramètres.

## Résultats numériques de la démonstration

- Architecture **3–5–2** : **25 connexions**.
- Sortie Softmax avec couche cachée décorée : environ `[0.805730, 0.194270]`.
- Apprentissage de `y = 0.6*x - 0.4` : poids **0.6**, biais **-0.4**, perte finale environ `8.47e-32`.
- Prédiction pour `[1.5]` : environ **0.5**.

Il ne s'agit pas d'un apprentissage XOR : les contraintes de poids positifs bornés limitent toujours les fonctions représentables.

## Régénérer le PDF et le ZIP

```powershell
python -m pip install -r tools/requirements-report.txt
python tools/build_deliverables.py
```

Faire d'abord `.\mvnw.cmd clean verify` pour inclure des résultats de tests à jour. Le script exclut `.git`, `target`, les journaux et les archives ZIP de l'archive finale. Il ajoute une copie des résultats JUnit dans `docs/resultats-tests.json`.

## Publication

- Ce projet V2 : [Neural-Network-Design-pattern-V2](https://github.com/CHAIMAEMOUHSSINE/Neural-Network-Design-pattern-V2).
- Dépôt d'origine (version antérieure) : [Neural-Network-design-patterns](https://github.com/CHAIMAEMOUHSSINE/Neural-Network-design-patterns).
- Activité AOP séparée : [Aspect-oriented-Programming-demo-](https://github.com/CHAIMAEMOUHSSINE/Aspect-oriented-Programming-demo-).

Les PDF et ZIP de la V2 sont disponibles dans `dist/` sur la branche `main` du dépôt dédié.

## Limites

Projet pédagogique synchrone, sans interface graphique, sans base de données ni session réseau. L'authentification est locale en mémoire ; l'inscription est du bootstrap de confiance, sans durcissement de production.

Les droits protègent les méthodes du réseau, pas l'exécution de code Java hostile détenant déjà des références. L'inscription des observateurs n'est pas une garantie de concurrence. Une exception d'observateur de type `RuntimeException` est isolée et comptabilisée ; une erreur fatale n'est pas promise comme récupérable.

Les classes d'activation peuvent être réutilisées hors réseau ; le réseau valide les dimensions et les valeurs avant de les appeler. La modification d'une activation ou d'un poids invalide un réseau entraîné et exige un nouvel apprentissage.
