package chaimae.net.tests;

import chaimae.net.activation.*;
import chaimae.net.activation.ActivationFactory.Type;
import chaimae.net.model.*;
import chaimae.net.observer.*;
import chaimae.net.state.NetworkState.Name;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class NetworkTest {
    @Test
    void builderValideAvantMutationEtPeutConstruireDesReseauxIndependants() {
        var builder = new NetworkBuilder().input(1).seed(17);
        assertThrows(IllegalArgumentException.class, () -> builder.hidden(0, Type.IDENTITY));
        assertThrows(NullPointerException.class, () -> builder.hidden(1, null));
        builder.output(1, Type.IDENTITY);
        var first = builder.build();
        var second = builder.build();
        assertEquals(2, first.layers().size());
        assertNotSame(first, second);
        assertNotSame(first.layers().getFirst().neurons().getFirst(),
                second.layers().getFirst().neurons().getFirst());
        assertEquals(first.connections().getFirst().weight(), second.connections().getFirst().weight());
    }

    private NeuralNetwork linear() {
        return new NetworkBuilder().input(2).output(1, Type.IDENTITY).seed(7).build();
    }

    private List<TrainingSample> data() {
        return List.of(new TrainingSample(new double[]{1, 0}, new double[]{0.25}),
                new TrainingSample(new double[]{0, 1}, new double[]{0.75}),
                new TrainingSample(new double[]{1, 1}, new double[]{1}));
    }

    @Test
    void architectureDeLaFigureContient25Connexions() {
        var network = new NetworkBuilder().input(3).hidden(5, Type.SIGMOID).output(2, Type.SOFTMAX).build();
        assertEquals(3, network.layers().size());
        assertEquals(25, network.connections().size());
        assertEquals(5, network.layers().getFirst().neurons().getFirst().outgoing().size());
        assertEquals(3, network.layers().get(1).neurons().getFirst().incoming().size());
        assertEquals(5, network.layers().getLast().neurons().getFirst().incoming().size());
        for (Connection connection : network.connections()) {
            assertTrue(connection.weight() >= 0 && connection.weight() <= 1);
            assertTrue(connection.source().outgoing().contains(connection));
            assertTrue(connection.destination().incoming().contains(connection));
        }
    }

    @Test
    void propagationDonneLesSommesPondereesAttendues() {
        var network = new NetworkBuilder().input(2).output(2, Type.IDENTITY).build();
        network.setWeight(0, 0.1);
        network.setWeight(1, 0.2);
        network.setWeight(2, 0.3);
        network.setWeight(3, 0.4);
        network.fit(List.of(new TrainingSample(new double[]{2, 3}, new double[]{1.1, 1.6})),
                new TrainingOptions(1, 1e-12));
        assertArrayEquals(new double[]{1.1, 1.6}, network.predict(new double[]{2, 3}), 1e-10);
    }

    @Test
    void apprentissageReelReduitLaPerteEtApprendUneFonctionConnue() {
        var network = linear();
        // Le biais ajoute un parametre a optimiser : garder la precision, augmenter les epochs.
        var result = network.train(data(), new TrainingOptions(1000, 0.1));
        assertTrue(result.finalLoss() < result.initialLoss());
        assertTrue(result.finalLoss() < 1e-12, result.toString());
        assertEquals(0.7, network.predict(new double[]{0.4, 0.8})[0], 1e-6);
        for (Connection connection : network.connections()) {
            assertTrue(connection.weight() >= 0 && connection.weight() <= 1);
        }
    }

    @Test
    void reseauCacheSoftmaxApprendEtNormaliseLaSortie() {
        var network = new NetworkBuilder().input(3).hidden(5, Type.SIGMOID).output(2, Type.SOFTMAX).build();
        var samples = List.of(new TrainingSample(new double[]{0.2, 0.4, 0.6}, new double[]{1, 0}));
        var result = network.fit(samples, new TrainingOptions(10, 0.05));
        assertTrue(result.finalLoss() < result.initialLoss());
        double[] output = network.predict(new double[]{0.2, 0.4, 0.6});
        assertEquals(1, output[0] + output[1], 1e-12);
    }

    @Test
    void etatsEtNotificationsInterdisentLesOperationsPendantTraining() {
        var network = linear();
        Dashboard dashboard = new Dashboard();
        network.subscribe(dashboard);
        assertEquals(Name.CONSTRUCTION, network.state());
        assertThrows(IllegalStateException.class, () -> network.predict(new double[]{1, 1}));
        AtomicInteger observed = new AtomicInteger();
        network.subscribe(change -> {
            if (change.to() == Name.TRAINING) {
                observed.incrementAndGet();
                assertEquals(Name.TRAINING, network.state());
                assertThrows(IllegalStateException.class, () -> network.predict(new double[]{1, 1}));
                assertThrows(IllegalStateException.class, () -> network.setWeight(0, 0.5));
                assertThrows(IllegalStateException.class, () -> network.train(data(), new TrainingOptions(1, 0.1)));
            }
        });
        network.fit(data(), new TrainingOptions(2, 0.1));
        assertEquals(1, observed.get());
        assertEquals(Name.TRAINED, network.state());
        assertEquals(List.of(Name.TRAINING, Name.TRAINED),
                dashboard.events().stream().map(StateChange::to).toList());
        assertEquals(1L, dashboard.statistics().get(Name.TRAINING));
        assertEquals(1L, dashboard.statistics().get(Name.TRAINED));
        network.train(data(), new TrainingOptions(1, 0.1));
        assertEquals(4, dashboard.events().size());
    }

    @Test
    void changerUneStrategieInvalideLeModeleEntraine() {
        var network = linear();
        network.fit(data(), new TrainingOptions(1, 0.1));
        network.setActivation(1, 0, new Sigmoid());
        assertEquals(Name.CONSTRUCTION, network.state());
        assertThrows(IllegalStateException.class, () -> network.predict(new double[]{1, 1}));
        network.fit(data(), new TrainingOptions(1, 0.1));
        network.setWeight(0, 0.25);
        assertEquals(Name.CONSTRUCTION, network.state());
    }

    @Test
    void desabonnementEtIsolationDesObservateursDefaillants() {
        var network = linear();
        Dashboard dashboard = new Dashboard();
        network.subscribe(dashboard);
        network.unsubscribe(dashboard);
        network.subscribe(change -> { throw new IllegalArgumentException("panne du tableau de bord"); });
        network.train(data(), new TrainingOptions(1, 0.1));
        assertEquals(Name.TRAINED, network.state());
        assertTrue(dashboard.events().isEmpty());
        assertEquals(2, network.observerFailures().size());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, 1.1, Double.NaN, Double.POSITIVE_INFINITY})
    void rejetteLesPoidsHorsIntervalle(double weight) {
        var network = linear();
        assertThrows(IllegalArgumentException.class, () -> network.setWeight(0, weight));
    }

    @Test
    void protectionsDesCollectionsEtDesDonnees() {
        var network = linear();
        assertThrows(UnsupportedOperationException.class, () -> network.layers().clear());
        assertThrows(UnsupportedOperationException.class, () -> network.connections().clear());
        assertThrows(UnsupportedOperationException.class, () -> network.layers().getFirst().neurons().clear());
        double[] input = {1, 2};
        TrainingSample sample = new TrainingSample(input, new double[]{3});
        input[0] = 99;
        assertEquals(1, sample.input()[0]);
        sample.input()[0] = 999;
        assertEquals(1, sample.input()[0]);
    }

    @Test
    void architectureEtSoftmaxMixteInvalides() {
        assertThrows(IllegalStateException.class, () -> new NetworkBuilder().input(2).build());
        assertThrows(IllegalArgumentException.class, () -> new NetworkBuilder().output(1, Type.IDENTITY).build());
        assertThrows(IllegalArgumentException.class, () -> new NetworkBuilder().input(0).output(1, Type.IDENTITY).build());
        var network = new NetworkBuilder().input(2).output(2, Type.IDENTITY).build();
        network.setActivation(1, 0, new Softmax());
        assertThrows(IllegalStateException.class, () -> network.fit(
                List.of(new TrainingSample(new double[]{1, 2}, new double[]{0, 1})), new TrainingOptions(1, 0.1)));
    }

    @Test
    void refuseDonneesEtOptionsInvalidesAvantTransition() {
        var network = linear();
        assertThrows(IllegalArgumentException.class, () -> new TrainingOptions(0, 0.1));
        assertThrows(IllegalArgumentException.class, () -> new TrainingOptions(1, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> network.fit(List.of(), new TrainingOptions(1, 0.1)));
        assertThrows(IllegalArgumentException.class, () -> network.fit(
                List.of(new TrainingSample(new double[]{1}, new double[]{1})), new TrainingOptions(1, 0.1)));
        assertThrows(IllegalArgumentException.class, () -> network.fit(
                List.of(new TrainingSample(new double[]{Double.NaN, 0}, new double[]{1})), new TrainingOptions(1, 0.1)));
        assertEquals(Name.CONSTRUCTION, network.state());
    }

    @Test
    void unePanneApresUneMiseAJourRestaureLesPoidsEtLetat() {
        ActivationFunction explosive = new ActivationFunction() {
            private int calls;
            @Override public double activate(double[] sums, int index) {
                if (++calls == 3) { throw new ArithmeticException("panne simulee"); }
                return sums[index];
            }
            @Override public double derivative(double[] sums, int output, int input) { return 1; }
        };
        var network = new NeuralNetwork();
        network.addLayer(LayerType.INPUT, 1, new Identity());
        network.addLayer(LayerType.OUTPUT, 1, explosive);
        network.connectFully(42);
        network.setWeight(0, 0.5);
        assertThrows(ArithmeticException.class, () -> network.fit(
                List.of(new TrainingSample(new double[]{1}, new double[]{0})), new TrainingOptions(2, 0.1)));
        assertEquals(0.5, network.connections().getFirst().weight());
        assertEquals(Name.CONSTRUCTION, network.state());
    }

    @Test
    void echecNumeriqueDunReentrainementRestaureTrained() {
        var network = linear();
        network.fit(data(), new TrainingOptions(1, 0.1));
        double[] before = network.predict(new double[]{1, 1});
        assertThrows(ArithmeticException.class, () -> network.fit(
                List.of(new TrainingSample(new double[]{1e308, 1e308}, new double[]{0})),
                new TrainingOptions(1, 0.1)));
        assertEquals(Name.TRAINED, network.state());
        assertArrayEquals(before, network.predict(new double[]{1, 1}));
    }
}
