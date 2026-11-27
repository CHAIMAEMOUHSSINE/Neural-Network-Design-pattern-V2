package chaimae.net.model;

/** Source et destination explicites ; seuls le réseau et l'apprentissage mutent le poids. */
public final class Connection {
    private final Neuron source;
    private final Neuron destination;
    private final int sourceLayer;
    private double weight;

    Connection(int sourceLayer, Neuron source, Neuron destination, double weight) {
        this.sourceLayer = sourceLayer;
        this.source = source;
        this.destination = destination;
        setWeight(weight);
    }

    public Neuron source() { return source; }
    public Neuron destination() { return destination; }
    public int sourceLayer() { return sourceLayer; }
    public double weight() { return weight; }

    void setWeight(double value) {
        if (!Double.isFinite(value) || value < 0 || value > 1) {
            throw new IllegalArgumentException("Le poids doit etre fini et compris entre 0 et 1.");
        }
        weight = value;
    }
}
