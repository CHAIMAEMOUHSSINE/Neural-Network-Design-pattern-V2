package chaimae.net.activation;

/**
 * Strategy avec contexte vectoriel : Softmax ne peut pas être une simple
 * fonction scalaire indépendante des autres neurones de la couche.
 */
public interface ActivationFunction {
    double activate(double[] sums, int index);

    /** Dérivée de la sortie outputIndex par rapport à la somme inputIndex. */
    double derivative(double[] sums, int outputIndex, int inputIndex);
}
