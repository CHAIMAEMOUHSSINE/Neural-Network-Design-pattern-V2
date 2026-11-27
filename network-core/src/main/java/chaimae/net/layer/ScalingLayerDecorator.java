package chaimae.net.layer;

import chaimae.net.activation.Softmax;

/** Met a l'echelle la sortie ET sa derivee : utilisable en apprentissage. */
public final class ScalingLayerDecorator extends LayerDecorator {
    private final double factor;

    public ScalingLayerDecorator(LayerComponent component, double factor) {
        super(component);
        if (!Double.isFinite(factor) || factor <= 0) {
            throw new IllegalArgumentException("Facteur strictement positif et fini requis.");
        }
        if (factor != 1 && component.neurons().stream().anyMatch(n -> n.activation() instanceof Softmax)) {
            throw new IllegalArgumentException("Ne pas denormaliser une couche Softmax.");
        }
        this.factor = factor;
    }

    public double factor() { return factor; }

    @Override
    public double[] activate(double[] sums) {
        double[] output = super.activate(sums);
        for (int i = 0; i < output.length; i++) {
            output[i] = checked(output[i] * factor);
        }
        return output;
    }

    @Override
    public double derivative(double[] sums, int outputIndex, int inputIndex) {
        return checked(factor * super.derivative(sums, outputIndex, inputIndex));
    }

    private static double checked(double value) {
        if (!Double.isFinite(value)) {
            throw new ArithmeticException("Debordement du decorateur de mise a l'echelle.");
        }
        return value;
    }
}
