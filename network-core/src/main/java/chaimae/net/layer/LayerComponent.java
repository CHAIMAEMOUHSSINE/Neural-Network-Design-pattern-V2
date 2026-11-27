package chaimae.net.layer;

import chaimae.net.model.LayerType;
import chaimae.net.model.Neuron;
import java.util.List;

/** Component du pattern Decorator. Les sommes incluent deja les biais. */
public interface LayerComponent {
    LayerType type();
    List<Neuron> neurons();
    double[] activate(double[] sums);
    double derivative(double[] sums, int outputIndex, int inputIndex);
}
