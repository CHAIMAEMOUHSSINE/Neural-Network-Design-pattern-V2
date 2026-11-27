package chaimae.net.model;

import chaimae.net.activation.ActivationFactory;
import chaimae.net.activation.ActivationFactory.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Builder : assemble des couches et centralise le câblage complet reproductible. */
public final class NetworkBuilder {
    private record Specification(LayerType type, int count, Type activation) {
        private Specification {
            Objects.requireNonNull(type, "Le type de couche est requis.");
            Objects.requireNonNull(activation, "Le type d'activation est requis.");
            if (count < 1) {
                throw new IllegalArgumentException("Une couche doit contenir au moins un neurone.");
            }
        }
    }

    private final List<Specification> specifications = new ArrayList<>();
    private long seed = 42;

    public NetworkBuilder input(int count) {
        specifications.add(new Specification(LayerType.INPUT, count, Type.IDENTITY));
        return this;
    }

    public NetworkBuilder hidden(int count, Type activation) {
        specifications.add(new Specification(LayerType.HIDDEN, count, activation));
        return this;
    }

    public NetworkBuilder output(int count, Type activation) {
        specifications.add(new Specification(LayerType.OUTPUT, count, activation));
        return this;
    }

    public NetworkBuilder seed(long seed) {
        this.seed = seed;
        return this;
    }

    public NeuralNetwork build() {
        NeuralNetwork network = new NeuralNetwork();
        for (Specification specification : specifications) {
            network.addLayer(specification.type(), specification.count(),
                    ActivationFactory.create(specification.activation()));
        }
        network.connectFully(seed);
        return network;
    }
}
