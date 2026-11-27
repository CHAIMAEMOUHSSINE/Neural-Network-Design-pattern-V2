package fr.enset.nn.tests;

import fr.enset.nn.activation.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ActivationTest {
    @Test
    void sigmoidUtiliseLaFormuleCorrigeeEtResteStable() {
        var sigmoid = new Sigmoid();
        assertEquals(0.5, sigmoid.activate(new double[]{0}, 0));
        assertEquals(1, sigmoid.activate(new double[]{1000}, 0));
        assertEquals(0, sigmoid.activate(new double[]{-1000}, 0));
        assertEquals(0.25, sigmoid.derivative(new double[]{0}, 0, 0));
        assertEquals(0, sigmoid.derivative(new double[]{1, 2}, 0, 1));
    }

    @Test
    void identityEtAdaptateurRespectentLeurContrat() {
        var identity = new Identity();
        assertEquals(-3, identity.activate(new double[]{-3}, 0));
        assertEquals(1, identity.derivative(new double[]{-3}, 0, 0));
        var adapted = new LegacyActivationAdapter(new AncienneFonctionTanh());
        assertEquals(Math.tanh(0.7), adapted.activate(new double[]{0.7}, 0));
        assertEquals(1 - Math.pow(Math.tanh(0.7), 2), adapted.derivative(new double[]{0.7}, 0, 0), 1e-12);
    }

    @Test
    void softmaxNormaliseSansDebordement() {
        double[] input = {1000, 1001, 999};
        var softmax = new Softmax();
        double total = 0;
        for (int i = 0; i < input.length; i++) {
            double probability = softmax.activate(input, i);
            assertTrue(Double.isFinite(probability) && probability > 0);
            total += probability;
        }
        assertEquals(1, total, 1e-12);
        assertEquals(1.0 / 3, softmax.activate(new double[]{0, 0, 0}, 1), 1e-12);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void jacobienneSoftmaxCorrespondAuxDifferencesFinies(int column) {
        double[] input = {0.2, 0.5, -0.7};
        var softmax = new Softmax();
        double step = 1e-6;
        for (int row = 0; row < input.length; row++) {
            double[] plus = input.clone();
            double[] minus = input.clone();
            plus[column] += step;
            minus[column] -= step;
            double numerical = (softmax.activate(plus, row) - softmax.activate(minus, row)) / (2 * step);
            assertEquals(numerical, softmax.derivative(input, row, column), 1e-8);
        }
    }

    @Test
    void fabriqueCreeLesQuatreStrategies() {
        assertInstanceOf(Identity.class, ActivationFactory.create(ActivationFactory.Type.IDENTITY));
        assertInstanceOf(Sigmoid.class, ActivationFactory.create(ActivationFactory.Type.SIGMOID));
        assertInstanceOf(Softmax.class, ActivationFactory.create(ActivationFactory.Type.SOFTMAX));
        assertInstanceOf(LegacyActivationAdapter.class, ActivationFactory.create(ActivationFactory.Type.LEGACY_TANH));
    }
}
