package chaimae.net.tests;

import chaimae.net.activation.ActivationFactory.Type;
import chaimae.net.layer.*;
import chaimae.net.model.*;
import chaimae.net.state.NetworkState.Name;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LayerDecoratorTest extends AuthenticatedNetworkTest {
    @Test
    void moyenneStatistiqueResteFiniePourDesSortiesFiniesExtremes() {
        var network = new NetworkBuilder().input(1).output(3, Type.IDENTITY).build();
        network.decorateLayer(1, StatisticsLayerDecorator::new);
        var stats = (StatisticsLayerDecorator) network.layers().getLast();
        double max = Double.MAX_VALUE;
        stats.activate(new double[]{max, max, max});
        assertEquals(max, stats.statistics().lastMean());
        stats.activate(new double[]{max, -max, 0});
        assertEquals(0, stats.statistics().lastMean());
        stats.activate(new double[]{0, 0, 0});
        assertEquals(0, stats.statistics().lastMean());
        assertEquals(3, stats.statistics().evaluations());
    }

    @Test
    void coucheValideLesDimensionsEtIndicesDuMemeContrat() {
        var network = new NetworkBuilder().input(1).output(2, Type.IDENTITY).build();
        var layer = network.layers().getLast();
        assertThrows(NullPointerException.class, () -> layer.activate(null));
        assertThrows(IllegalArgumentException.class, () -> layer.activate(new double[]{1}));
        assertThrows(IllegalArgumentException.class, () -> layer.derivative(new double[]{1}, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> layer.derivative(new double[]{1, 2}, 0, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> layer.derivative(new double[]{1, 2}, -1, 0));
        assertArrayEquals(new double[]{1, 2}, layer.activate(new double[]{1, 2}));
    }

    private NeuralNetwork network() {
        var network = new NetworkBuilder().input(1).hidden(1, Type.IDENTITY).output(1, Type.IDENTITY).build();
        network.setWeight(0, 0.4);
        network.setWeight(1, 0.6);
        network.setBias(1, 0, 0.1);
        network.setBias(2, 0, -0.2);
        return network;
    }

    private void train(NeuralNetwork network) {
        network.fit(List.of(new TrainingSample(new double[]{0.7}, new double[]{0.3})),
                new TrainingOptions(1, 0.01));
    }

    @Test
    void empileEtRetireDesDecorateursSansDupliquerLesNeurones() {
        var network = network();
        var base = network.layers().get(1);
        network.decorateLayer(1, StatisticsLayerDecorator::new);
        var stats = network.layers().get(1);
        network.decorateLayer(1, layer -> new ScalingLayerDecorator(layer, 3));
        assertSame(stats, ((LayerDecorator) network.layers().get(1)).component());
        assertSame(base.neurons().getFirst(), network.layers().get(1).neurons().getFirst());
        assertEquals(2, network.connections().size());
        network.undecorateLayer(1);
        assertSame(stats, network.layers().get(1));
        network.undecorateLayer(1);
        assertSame(base, network.layers().get(1));
        assertThrows(IllegalStateException.class, () -> network.undecorateLayer(1));
    }

    @Test
    void statistiquesNeChangentNiLaPredictionNiLesGradients() {
        var plain = network();
        var decorated = network();
        decorated.decorateLayer(1, StatisticsLayerDecorator::new);
        train(plain);
        train(decorated);
        assertArrayEquals(plain.predict(new double[]{0.7}), decorated.predict(new double[]{0.7}), 1e-15);
        for (int i = 0; i < 2; i++) {
            assertEquals(plain.connections().get(i).weight(), decorated.connections().get(i).weight());
        }
        var stats = (StatisticsLayerDecorator) decorated.layers().get(1);
        assertEquals(4, stats.statistics().evaluations()); // perte initiale, update, perte finale, predict
        assertEquals(plain.layers().get(1).activate(new double[]{0.2})[0],
                stats.activate(new double[]{0.2})[0]);
        assertEquals(0.2, stats.statistics().lastMean(), 1e-15);
    }

    @Test
    void scalingParticipeAuCalculEtALaRetropropagationDesPoidsEtBiais() {
        var network = network();
        network.decorateLayer(1, layer -> new ScalingLayerDecorator(layer, 3));
        train(network);
        // Avant mise a jour : aHidden=3*(0.4*0.7+0.1)=1.14 ; y=0.484 ; erreur=0.184.
        assertEquals(0.4 - 0.01 * 0.184 * 0.6 * 3 * 0.7, network.connections().get(0).weight(), 1e-12);
        assertEquals(0.6 - 0.01 * 0.184 * 1.14, network.connections().get(1).weight(), 1e-12);
        assertEquals(0.1 - 0.01 * 0.184 * 0.6 * 3,
                network.layers().get(1).neurons().getFirst().bias(), 1e-12);
        assertEquals(-0.2 - 0.01 * 0.184, network.layers().get(2).neurons().getFirst().bias(), 1e-12);
    }

    @Test
    void deriveeDuDecorateurCorrespondAuxDifferencesFinies() {
        var network = new NetworkBuilder().input(1).hidden(2, Type.SIGMOID).output(1, Type.IDENTITY).build();
        network.decorateLayer(1, layer -> new ScalingLayerDecorator(layer, 2.5));
        var layer = network.layers().get(1);
        double[] z = {0.3, -0.7};
        double h = 1e-6;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                double[] plus = z.clone();
                double[] minus = z.clone();
                plus[j] += h;
                minus[j] -= h;
                assertEquals((layer.activate(plus)[i] - layer.activate(minus)[i]) / (2*h),
                        layer.derivative(z, i, j), 1e-8);
            }
        }
    }

    @Test
    void changementDynamiqueInvalideLeStatutTrained() {
        var network = network();
        train(network);
        network.decorateLayer(1, StatisticsLayerDecorator::new);
        assertEquals(Name.CONSTRUCTION, network.state());
        assertThrows(IllegalStateException.class, () -> network.predict(new double[]{1}));
        train(network);
        network.undecorateLayer(1);
        assertEquals(Name.CONSTRUCTION, network.state());
    }

    @Test
    void refuseUneDecorationPendantTraining() {
        var network = network();
        network.decorateLayer(1, StatisticsLayerDecorator::new);
        network.subscribe(event -> {
            if (event.to() == Name.TRAINING) {
                assertThrows(IllegalStateException.class, () -> network.decorateLayer(1, StatisticsLayerDecorator::new));
                assertThrows(IllegalStateException.class, () -> network.undecorateLayer(1));
            }
        });
        train(network);
    }

    @Test
    void refuseUnComposantEtrangerOuUneFausseDecoration() {
        var network = network();
        var old = network.layers().get(1);
        var other = network();
        assertThrows(IllegalArgumentException.class, () -> network.decorateLayer(1, layer -> layer));
        assertThrows(IllegalArgumentException.class, () -> network.decorateLayer(1,
                layer -> new StatisticsLayerDecorator(other.layers().get(1))));
        assertThrows(NullPointerException.class, () -> network.decorateLayer(1, layer -> null));
        assertSame(old, network.layers().get(1));
    }

    @Test
    void laCoucheEntreePeutAussiEtreDecoree() {
        var network = new NetworkBuilder().input(1).output(1, Type.IDENTITY).build();
        network.setWeight(0, 0.25);
        network.setBias(1, 0, -0.1);
        network.decorateLayer(0, layer -> new ScalingLayerDecorator(layer, 3));
        network.decorateLayer(0, StatisticsLayerDecorator::new);
        network.fit(List.of(new TrainingSample(new double[]{2}, new double[]{1.4})),
                new TrainingOptions(1, 0.1));
        assertEquals(1.4, network.predict(new double[]{2})[0], 1e-12);
        assertEquals(4, ((StatisticsLayerDecorator) network.layers().getFirst()).statistics().evaluations());
    }

    @Test
    void valideLeFacteurEtPreserveLaNormalisationSoftmax() {
        var network = network();
        for (double value : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> network.decorateLayer(1,
                    layer -> new ScalingLayerDecorator(layer, value)));
        }
        var softmax = new NetworkBuilder().input(1).output(2, Type.SOFTMAX).build();
        assertThrows(IllegalArgumentException.class, () -> softmax.decorateLayer(1,
                layer -> new ScalingLayerDecorator(layer, 2)));
        softmax.decorateLayer(1, StatisticsLayerDecorator::new);
        softmax.fit(List.of(new TrainingSample(new double[]{1}, new double[]{1, 0})),
                new TrainingOptions(1, 0.1));
        var result = softmax.predict(new double[]{1});
        assertEquals(1, result[0] + result[1], 1e-12);
    }

    @Test
    void unDepassementDansUnDecorateurRestaureLesParametres() {
        var network = network();
        network.decorateLayer(1, layer -> new ScalingLayerDecorator(layer, Double.MAX_VALUE));
        assertThrows(ArithmeticException.class, () -> network.fit(
                List.of(new TrainingSample(new double[]{10}, new double[]{0})), new TrainingOptions(1, 0.1)));
        assertEquals(0.4, network.connections().get(0).weight());
        assertEquals(0.1, network.layers().get(1).neurons().getFirst().bias());
        assertEquals(Name.CONSTRUCTION, network.state());
    }
}
