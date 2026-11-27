package fr.enset.nn.model;

import fr.enset.nn.activation.ActivationFunction;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Layer {
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
}
