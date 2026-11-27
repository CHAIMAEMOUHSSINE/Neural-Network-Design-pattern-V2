package chaimae.net.demo;

import chaimae.net.activation.ActivationFactory.Type;
import chaimae.net.annotation.Role;
import chaimae.net.layer.StatisticsLayerDecorator;
import chaimae.net.model.NetworkBuilder;
import chaimae.net.model.NeuralNetwork;
import chaimae.net.model.TrainingOptions;
import chaimae.net.model.TrainingSample;
import chaimae.net.security.AuthenticationService;
import chaimae.net.security.SecurityContext;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.LogManager;

public final class DemoSecurisee {
    static {
        try (var configuration = DemoSecurisee.class.getResourceAsStream("/logging.properties")) {
            if (configuration == null) {
                throw new IllegalStateException("Configuration des logs manquante.");
            }
            LogManager.getLogManager().readConfiguration(configuration);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DemoSecurisee() {
    }

    public static void main(String[] args) {
        AuthenticationService auth = new AuthenticationService();
        char[] password = "Demo-2027!".toCharArray(); // Compte fictif local, pas un secret réel.
        try {
            auth.register("admin", password, EnumSet.allOf(Role.class));
            auth.register("lecteur", password, Set.of(Role.READ));
            try {
                new NetworkBuilder().input(1).output(1, Type.IDENTITY).build();
            } catch (SecurityException error) {
                System.out.println("Anonyme : " + error.getMessage());
            }
            auth.authenticate("admin", password);
            NeuralNetwork network = new NetworkBuilder().input(1).output(1, Type.IDENTITY).seed(42).build();
            network.setBias(1, 0, -0.2);
            network.decorateLayer(1, StatisticsLayerDecorator::new);
            network.fit(List.of(new TrainingSample(new double[]{1}, new double[]{0.25})),
                    new TrainingOptions(2, 0.1));
            System.out.println("Administrateur : " + Arrays.toString(network.predict(new double[]{0.8})));
            auth.authenticate("lecteur", password);
            System.out.println("Lecture de l'etat : " + network.state());
            try {
                network.predict(new double[]{0.8});
            } catch (SecurityException error) {
                System.out.println("Lecteur : " + error.getMessage());
            }
            System.out.println("Audit FINE : neural-network-audit-*.log dans le repertoire courant.");
        } finally {
            SecurityContext.logout();
            Arrays.fill(password, '\0');
        }
    }
}
