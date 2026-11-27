package chaimae.net.model;

import chaimae.net.activation.ActivationFunction;
import chaimae.net.layer.LayerComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Layer implements LayerComponent {
    private final LayerType type;
    private final List<Neuron> neurons;

    Layer(LayerType type, int count, ActivationFunction activation) {
        this.type = Objects.requireNonNull(type);
        if (count < 1) {
            throw new IllegalArgumentException("Une couche doit contenir au moins un neurone.");
        }
        List<Neuron> values = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            values.add(new Neuron(index, activation));
        }
        neurons = List.copyOf(values);
    }

    public LayerType type() { return type; }
    public List<Neuron> neurons() { return neurons; }

    @Override
    public double[] activate(double[] sums) {
        requireDimension(sums);
        double[] output = new double[sums.length];
        for (Neuron neuron : neurons) {
            output[neuron.index()] = neuron.activation().activate(sums, neuron.index());
        }
        return output;
    }

    @Override
    public double derivative(double[] sums, int outputIndex, int inputIndex) {
        requireDimension(sums);
        Objects.checkIndex(outputIndex, neurons.size());
        Objects.checkIndex(inputIndex, neurons.size());
        return neurons.get(outputIndex).activation().derivative(sums, outputIndex, inputIndex);
    }

    private void requireDimension(double[] sums) {
        Objects.requireNonNull(sums, "Le vecteur des sommes est requis.");
        if (sums.length != neurons.size()) {
            throw new IllegalArgumentException("Dimension de couche incorrecte.");
        }
    }
}
