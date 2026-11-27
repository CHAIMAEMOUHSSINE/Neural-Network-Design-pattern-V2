package fr.enset.nn.model;

import fr.enset.nn.activation.ActivationFunction;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;

public final class Neuron {
    private final int index;
    private ActivationFunction activation;
    private final List<Connection> incoming = new ArrayList<>();
    private final List<Connection> outgoing = new ArrayList<>();

    Neuron(int index, ActivationFunction activation) {
        this.index = index;
        this.activation = Objects.requireNonNull(activation);
    }

    public int index() { return index; }
    public ActivationFunction activation() { return activation; }
    public List<Connection> incoming() { return List.copyOf(incoming); }
    public List<Connection> outgoing() { return List.copyOf(outgoing); }

    void attachIncoming(Connection connection) { incoming.add(connection); }
    void attachOutgoing(Connection connection) { outgoing.add(connection); }
    void clearConnections() { incoming.clear(); outgoing.clear(); }

    void setActivation(ActivationFunction activation) {
        this.activation = Objects.requireNonNull(activation);
    }
}
