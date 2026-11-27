package chaimae.net.tests;

import chaimae.net.activation.ActivationFactory.Type;
import chaimae.net.activation.ActivationFunction;
import chaimae.net.model.*;
import chaimae.net.state.NetworkState.Name;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class BiasTest extends AuthenticatedNetworkTest {
    private NeuralNetwork network() {
        return new NetworkBuilder().input(1).output(1, Type.IDENTITY).build();
    }

    @Test
    void chaqueNeuronePossedeUnBiaisInitialementNul() {
        var network = new NetworkBuilder().input(3).hidden(5, Type.SIGMOID).output(2, Type.SOFTMAX).build();
        network.layers().forEach(layer -> layer.neurons().forEach(n -> assertEquals(0, n.bias())));
    }

    @Test
    void propagationAjouteLeBiaisALaSommePonderee() {
        var network = network();
        network.setWeight(0, 0.25);
        network.setBias(1, 0, -0.3);
        network.fit(List.of(new TrainingSample(new double[]{2}, new double[]{0.2})),
                new TrainingOptions(1, 0.1));
        assertEquals(0.2, network.predict(new double[]{2})[0], 1e-12);
    }

    @Test
    void apprendUneRelationAffineAvecOrdonnéeALOrigineNonNulle() {
        var network = network();
        var samples = List.of(
                new TrainingSample(new double[]{0}, new double[]{-0.4}),
                new TrainingSample(new double[]{1}, new double[]{0.2}),
                new TrainingSample(new double[]{2}, new double[]{0.8}));
        var result = network.fit(samples, new TrainingOptions(1000, 0.05));
        assertTrue(result.finalLoss() < 1e-16, result.toString());
        assertEquals(0.6, network.connections().getFirst().weight(), 1e-8);
        assertEquals(-0.4, network.layers().getLast().neurons().getFirst().bias(), 1e-8);
        assertEquals(0.5, network.predict(new double[]{1.5})[0], 1e-8);
    }

    @Test
    void changerLeBiaisInvalideLeModele() {
        var network = network();
        network.fit(List.of(new TrainingSample(new double[]{0}, new double[]{0})),
                new TrainingOptions(1, 0.1));
        network.setBias(1, 0, 0.7);
        assertEquals(Name.CONSTRUCTION, network.state());
        assertThrows(IllegalStateException.class, () -> network.predict(new double[]{0}));
        assertThrows(IllegalArgumentException.class, () -> network.setBias(0, 0, 1));
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejetteUnBiaisNonFini(double value) {
        var network = network();
        assertThrows(IllegalArgumentException.class, () -> network.setBias(1, 0, value));
        assertEquals(0, network.layers().getLast().neurons().getFirst().bias());
    }

    @Test
    void echecApresUneMiseAJourRestaureAussiLesBiais() {
        var network = network();
        network.setWeight(0, 0.5);
        network.setBias(1, 0, -0.2);
        network.setActivation(1, 0, new ActivationFunction() {
            private int calls;
            @Override public double activate(double[] sums, int index) {
                if (++calls == 3) { throw new ArithmeticException("panne apres mise a jour"); }
                return sums[index];
            }
            @Override public double derivative(double[] sums, int output, int input) { return 1; }
        });
        assertThrows(ArithmeticException.class, () -> network.fit(
                List.of(new TrainingSample(new double[]{1}, new double[]{0})), new TrainingOptions(2, 0.1)));
        assertEquals(0.5, network.connections().getFirst().weight());
        assertEquals(-0.2, network.layers().getLast().neurons().getFirst().bias());
        assertEquals(Name.CONSTRUCTION, network.state());
    }

    @Test
    void leBiaisNePeutPasChangerPendantTraining() {
        var network = network();
        network.subscribe(event -> {
            if (event.to() == Name.TRAINING) {
                assertThrows(IllegalStateException.class, () -> network.setBias(1, 0, 1));
            }
        });
        network.fit(List.of(new TrainingSample(new double[]{0}, new double[]{0})),
                new TrainingOptions(1, 0.1));
    }
}
