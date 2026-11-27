package chaimae.net.layer;

import chaimae.net.model.LayerType;
import chaimae.net.model.Neuron;
import java.util.List;
import java.util.Objects;

/** Le decorateur conserve les memes neurones, connexions et type de couche. */
public abstract class LayerDecorator implements LayerComponent {
    private final LayerComponent component;

    protected LayerDecorator(LayerComponent component) {
        this.component = Objects.requireNonNull(component);
    }

    public final LayerComponent component() { return component; }
    @Override public final LayerType type() { return component.type(); }
    @Override public final List<Neuron> neurons() { return component.neurons(); }
    @Override public double[] activate(double[] sums) { return component.activate(sums); }
    @Override public double derivative(double[] sums, int outputIndex, int inputIndex) {
        return component.derivative(sums, outputIndex, inputIndex);
    }
}
