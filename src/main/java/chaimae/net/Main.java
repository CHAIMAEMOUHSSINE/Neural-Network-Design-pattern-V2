package chaimae.net;

import chaimae.net.activation.ActivationFactory.Type;
import chaimae.net.annotation.Role;
import chaimae.net.layer.ScalingLayerDecorator;
import chaimae.net.layer.StatisticsLayerDecorator;
import chaimae.net.model.NetworkBuilder;
import chaimae.net.model.NeuralNetwork;
import chaimae.net.model.TrainingOptions;
import chaimae.net.model.TrainingSample;
import chaimae.net.observer.Dashboard;
import chaimae.net.security.AuthenticationService;
import chaimae.net.security.SecurityContext;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.LogManager;

/** Point d'entree unique : modele, apprentissage, decorateurs et securite. */
public final class Main {
    static {
        try (var configuration = Main.class.getResourceAsStream("/logging.properties")) {
            if (configuration == null) {
                throw new IllegalStateException("Configuration des logs manquante.");
            }
            LogManager.getLogManager().readConfiguration(configuration);
        } catch (IOException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    private Main() {
    }

    public static void main(String[] args) {
        var authentication = new AuthenticationService();
        char[] password = "Demo-2027!".toCharArray(); // Comptes fictifs de demonstration.
        try {
            authentication.register("admin", password, EnumSet.allOf(Role.class));
            authentication.register("lecteur", password, Set.of(Role.READ));
            try {
                new NetworkBuilder().input(1).output(1, Type.IDENTITY).build();
            } catch (SecurityException error) {
                System.out.println("Anonyme : " + error.getMessage());
            }
            authentication.authenticate("admin", password);
            demonstrateTopology();
            NeuralNetwork network = demonstrateTraining();
            authentication.authenticate("lecteur", password);
            System.out.println("Lecture de l'etat : " + network.state());
            try {
                network.predict(new double[]{1.5});
            } catch (SecurityException error) {
                System.out.println("Lecteur : " + error.getMessage());
            }
            System.out.println("Audit FINE : neural-network-audit-*.log dans le repertoire courant.");
        } finally {
            SecurityContext.logout();
            Arrays.fill(password, '\0');
        }
    }

    private static void demonstrateTopology() {
        var network = new NetworkBuilder().input(3).hidden(5, Type.SIGMOID)
                .output(2, Type.SOFTMAX).seed(42).build();
        network.decorateLayer(1, layer -> new ScalingLayerDecorator(layer, 1.1));
        network.decorateLayer(1, StatisticsLayerDecorator::new);
        network.fit(List.of(new TrainingSample(new double[]{0.2, 0.4, 0.6}, new double[]{1, 0})),
                new TrainingOptions(10, 0.05));
        System.out.println("Architecture 3-5-2 : " + network.connections().size() + " connexions");
        System.out.println("Sortie Softmax : " + Arrays.toString(network.predict(new double[]{0.2, 0.4, 0.6})));
        System.out.println("Couche cachee decoree : "
                + ((StatisticsLayerDecorator) network.layers().get(1)).statistics());
    }

    private static NeuralNetwork demonstrateTraining() {
        var network = new NetworkBuilder().input(1).output(1, Type.IDENTITY).seed(7).build();
        var dashboard = new Dashboard();
        network.subscribe(dashboard);
        var samples = List.of(
                new TrainingSample(new double[]{0}, new double[]{-0.4}),
                new TrainingSample(new double[]{1}, new double[]{0.2}),
                new TrainingSample(new double[]{2}, new double[]{0.8}));
        System.out.println("Apprentissage reel : " + network.train(samples, new TrainingOptions(1000, 0.05)));
        System.out.println("Poids appris : " + network.connections().getFirst().weight());
        System.out.println("Biais appris : " + network.layers().getLast().neurons().getFirst().bias());
        System.out.println("Prediction [1.5] : " + Arrays.toString(network.predict(new double[]{1.5})));
        System.out.println("Notifications : " + dashboard.events());
        System.out.println("Statistiques : " + dashboard.statistics());
        return network;
    }
}
