# Examen V2 — Réseau de neurones, Design Patterns et AOP

Solution Java du PDF **Examen Design Pattern V2** : réseau feed-forward avec **biais entraînables**, apprentissage réel, **Strategy, Adapter, State, Observer, Decorator**, complétés par **Builder** et **Simple Factory**.

Package Java : **`chaimae.net`**.

## Organisation

- Modèle indépendant : `network-core/`.
- Aspects de journalisation et sécurité : `network-aop/`.
- Tests JUnit : `src/test/java/` dans chaque module.

Le dépôt publie le code, les tests et les fichiers Maven nécessaires. Les rapports, archives et outils locaux de génération ne sont pas suivis par Git.

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

## Limites

Projet pédagogique synchrone, sans interface graphique, sans base de données ni session réseau. L'authentification est locale en mémoire ; l'inscription est du bootstrap de confiance, sans durcissement de production.

Les droits protègent les méthodes du réseau, pas une JVM exécutant du code hostile. Une RuntimeException d'observateur est isolée, pas une erreur fatale. Toute reconfiguration exige un nouvel entraînement.
