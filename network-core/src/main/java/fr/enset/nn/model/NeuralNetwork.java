package fr.enset.nn.model;

import fr.enset.nn.activation.ActivationFunction;
import fr.enset.nn.activation.Identity;
import fr.enset.nn.activation.Softmax;
import fr.enset.nn.annotation.RequiresRole;
import fr.enset.nn.annotation.Role;
import fr.enset.nn.observer.NetworkObserver;
import fr.enset.nn.observer.StateChange;
import fr.enset.nn.state.ConstructionState;
import fr.enset.nn.state.NetworkState;
import fr.enset.nn.state.TrainedState;
import fr.enset.nn.state.TrainingState;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * Réseau dense feed-forward sans biais. Apprentissage SGD par rétropropagation,
 * avec projection des poids dans [0,1], conformément à l'énoncé.
 * Modèle pédagogique synchrone, non conçu pour les accès concurrents.
 */
public final class NeuralNetwork {
    private final List<Layer> layers = new ArrayList<>();
    private final List<Connection> connections = new ArrayList<>();
    private final Set<NetworkObserver> observers = new LinkedHashSet<>();
    private final List<String> observerFailures = new ArrayList<>();
    private NetworkState state = new ConstructionState();

    @RequiresRole(Role.READ)
    public NetworkState.Name state() { return state.name(); }

    @RequiresRole(Role.READ)
    public List<Layer> layers() { return List.copyOf(layers); }

    @RequiresRole(Role.READ)
    public List<Connection> connections() { return List.copyOf(connections); }

    @RequiresRole(Role.READ)
    public List<String> observerFailures() { return List.copyOf(observerFailures); }

    @RequiresRole(Role.READ)
    public void subscribe(NetworkObserver observer) { observers.add(Objects.requireNonNull(observer)); }

    @RequiresRole(Role.READ)
    public void unsubscribe(NetworkObserver observer) { observers.remove(observer); }

    @RequiresRole(Role.CONFIGURE)
    public void addLayer(LayerType type, int count, ActivationFunction activation) {
        state.requireConfiguration();
        Objects.requireNonNull(type);
        Objects.requireNonNull(activation);
        if (layers.isEmpty() && type != LayerType.INPUT
                || !layers.isEmpty() && type == LayerType.INPUT
                || !layers.isEmpty() && layers.getLast().type() == LayerType.OUTPUT) {
            throw new IllegalArgumentException("Ordre requis : INPUT, HIDDEN*, OUTPUT.");
        }
        if (type == LayerType.INPUT && !(activation instanceof Identity)) {
            throw new IllegalArgumentException("La couche d'entree transmet directement les entrees.");
        }
        layers.add(new Layer(type, count, activation));
        invalidate();
    }

    @RequiresRole(Role.CONFIGURE)
    public void connectFully(long seed) {
        state.requireConfiguration();
        validateArchitecture();
        connections.clear();
        layers.forEach(l -> l.neurons().forEach(Neuron::clearConnections));
        Random random = new Random(seed);
        for (int layer = 0; layer < layers.size() - 1; layer++) {
            for (Neuron source : layers.get(layer).neurons()) {
                for (Neuron destination : layers.get(layer + 1).neurons()) {
                    Connection connection = new Connection(layer, source, destination, random.nextDouble());
                    connections.add(connection);
                    source.attachOutgoing(connection);
                    destination.attachIncoming(connection);
                }
            }
        }
        invalidate();
    }

    @RequiresRole(Role.CONFIGURE)
    public void setWeight(int connectionIndex, double weight) {
        state.requireConfiguration();
        connections.get(connectionIndex).setWeight(weight);
        invalidate();
    }

    @RequiresRole(Role.CONFIGURE)
    public void setActivation(int layerIndex, int neuronIndex, ActivationFunction activation) {
        state.requireConfiguration();
        if (layerIndex == 0) {
            throw new IllegalArgumentException("Activation de la couche d'entree fixe.");
        }
        layers.get(layerIndex).neurons().get(neuronIndex).setActivation(activation);
        invalidate();
    }

    @RequiresRole(Role.PREDICT)
    public double[] predict(double[] input) {
        state.requirePrediction();
        return forward(input).values().getLast().clone();
    }

    @RequiresRole(Role.TRAIN)
    public TrainingResult fit(List<TrainingSample> samples, TrainingOptions options) {
        return train(samples, options);
    }

    @RequiresRole(Role.TRAIN)
    public TrainingResult train(List<TrainingSample> samples, TrainingOptions options) {
        state.requireTraining();
        validateArchitecture();
        validateConnections();
        Objects.requireNonNull(options);
        List<TrainingSample> data = List.copyOf(samples);
        if (data.isEmpty()) {
            throw new IllegalArgumentException("Jeu d'entrainement vide.");
        }
        for (TrainingSample sample : data) {
            validateVector(sample.input(), layers.getFirst().neurons().size());
            validateVector(sample.target(), layers.getLast().neurons().size());
        }
        double[] snapshot = connections.stream().mapToDouble(Connection::weight).toArray();
        NetworkState previous = state;
        try {
            transition(new TrainingState(), "Debut de l'entrainement");
            double initial = loss(data);
            for (int epoch = 0; epoch < options.epochs(); epoch++) {
                for (TrainingSample sample : data) {
                    update(sample, options.learningRate());
                }
            }
            double last = loss(data);
            transition(new TrainedState(), "Entrainement termine");
            return new TrainingResult(initial, last, options.epochs());
        } catch (RuntimeException error) {
            for (int i = 0; i < connections.size(); i++) {
                connections.get(i).setWeight(snapshot[i]);
            }
            transition(previous, "Entrainement annule : poids et etat restaures");
            throw error;
        }
    }

    private record ForwardPass(List<double[]> sums, List<double[]> values) {
    }

    private ForwardPass forward(double[] input) {
        validateVector(input, layers.getFirst().neurons().size());
        List<double[]> sums = new ArrayList<>();
        List<double[]> values = new ArrayList<>();
        sums.add(input.clone());
        values.add(input.clone());
        for (int layerIndex = 1; layerIndex < layers.size(); layerIndex++) {
            Layer layer = layers.get(layerIndex);
            double[] z = new double[layer.neurons().size()];
            double[] y = new double[z.length];
            for (Neuron neuron : layer.neurons()) {
                double sum = 0;
                for (Connection connection : neuron.incoming()) {
                    sum += connection.weight() * values.get(layerIndex - 1)[connection.source().index()];
                }
                z[neuron.index()] = finite(sum);
            }
            for (Neuron neuron : layer.neurons()) {
                y[neuron.index()] = finite(neuron.activation().activate(z, neuron.index()));
            }
            sums.add(z);
            values.add(y);
        }
        return new ForwardPass(sums, values);
    }

    private void update(TrainingSample sample, double learningRate) {
        ForwardPass pass = forward(sample.input());
        int last = layers.size() - 1;
        double[][] deltas = new double[layers.size()][];
        for (int layerIndex = last; layerIndex >= 1; layerIndex--) {
            Layer layer = layers.get(layerIndex);
            double[] outputGradient = new double[layer.neurons().size()];
            for (Neuron neuron : layer.neurons()) {
                int index = neuron.index();
                if (layerIndex == last) {
                    outputGradient[index] = pass.values().getLast()[index] - sample.target()[index];
                } else {
                    for (Connection connection : neuron.outgoing()) {
                        outputGradient[index] += connection.weight()
                                * deltas[layerIndex + 1][connection.destination().index()];
                    }
                }
            }
            deltas[layerIndex] = new double[outputGradient.length];
            for (int inputIndex = 0; inputIndex < outputGradient.length; inputIndex++) {
                double gradient = 0;
                for (Neuron outputNeuron : layer.neurons()) {
                    gradient += outputGradient[outputNeuron.index()]
                            * outputNeuron.activation().derivative(
                                    pass.sums().get(layerIndex), outputNeuron.index(), inputIndex);
                }
                deltas[layerIndex][inputIndex] = finite(gradient);
            }
        }
        // Toutes les dérivées sont calculées avant la première mutation des poids.
        for (Connection connection : connections) {
            double gradient = deltas[connection.sourceLayer() + 1][connection.destination().index()]
                    * pass.values().get(connection.sourceLayer())[connection.source().index()];
            double updated = finite(connection.weight() - learningRate * gradient);
            connection.setWeight(Math.max(0, Math.min(1, updated)));
        }
    }

    private double loss(List<TrainingSample> samples) {
        double total = 0;
        for (TrainingSample sample : samples) {
            double[] output = forward(sample.input()).values().getLast();
            double[] expected = sample.target();
            for (int i = 0; i < output.length; i++) {
                double error = output[i] - expected[i];
                total = finite(total + 0.5 * error * error);
            }
        }
        return total / samples.size();
    }

    private void validateArchitecture() {
        if (layers.size() < 2 || layers.getFirst().type() != LayerType.INPUT
                || layers.getLast().type() != LayerType.OUTPUT) {
            throw new IllegalStateException("Une couche d'entree et une couche de sortie sont requises.");
        }
        for (Layer layer : layers) {
            long count = layer.neurons().stream().filter(n -> n.activation() instanceof Softmax).count();
            if (count != 0 && count != layer.neurons().size()) {
                throw new IllegalStateException("Softmax doit couvrir toute sa couche.");
            }
        }
    }

    private void validateConnections() {
        int expected = 0;
        for (int layer = 0; layer < layers.size() - 1; layer++) {
            expected += layers.get(layer).neurons().size() * layers.get(layer + 1).neurons().size();
        }
        if (connections.size() != expected) {
            throw new IllegalStateException("Appeler connectFully avant l'entrainement.");
        }
    }

    private static void validateVector(double[] vector, int expected) {
        Objects.requireNonNull(vector);
        if (vector.length != expected) {
            throw new IllegalArgumentException("Dimension du vecteur incorrecte.");
        }
        for (double value : vector) {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("Donnees non finies interdites.");
            }
        }
    }

    private static double finite(double value) {
        if (!Double.isFinite(value)) {
            throw new ArithmeticException("Debordement numerique.");
        }
        return value;
    }

    private void invalidate() {
        if (state.name() == NetworkState.Name.TRAINED) {
            transition(new ConstructionState(), "Configuration modifiee : nouvel entrainement requis");
        }
    }

    private void transition(NetworkState next, String reason) {
        NetworkState.Name from = state.name();
        state = next;
        if (from != next.name()) {
            StateChange event = new StateChange(from, next.name(), reason);
            for (NetworkObserver observer : List.copyOf(observers)) {
                try {
                    observer.onStateChange(event);
                } catch (RuntimeException error) {
                    // Un tableau de bord défaillant ne corrompt pas l'état métier.
                    observerFailures.add(error.getClass().getSimpleName());
                }
            }
        }
    }
}
