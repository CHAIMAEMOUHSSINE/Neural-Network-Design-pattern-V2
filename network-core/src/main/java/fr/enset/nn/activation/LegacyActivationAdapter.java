package fr.enset.nn.activation;

import java.util.Objects;

public final class LegacyActivationAdapter implements ActivationFunction {
    private final AncienneFonctionTanh ancienne;

    public LegacyActivationAdapter(AncienneFonctionTanh ancienne) {
        this.ancienne = Objects.requireNonNull(ancienne);
    }

    @Override
    public double activate(double[] sums, int index) {
        return ancienne.calculer(sums[index]);
    }

    @Override
    public double derivative(double[] sums, int outputIndex, int inputIndex) {
        return outputIndex == inputIndex ? ancienne.derivee(sums[outputIndex]) : 0;
    }
}
