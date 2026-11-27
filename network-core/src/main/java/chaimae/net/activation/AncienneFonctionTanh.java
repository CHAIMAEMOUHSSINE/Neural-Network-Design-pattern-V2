package chaimae.net.activation;

/** API historique incompatible avec ActivationFunction. */
public final class AncienneFonctionTanh implements LegacyActivation {
    public double calculer(double valeur) {
        return Math.tanh(valeur);
    }

    public double derivee(double valeur) {
        double y = calculer(valeur);
        return 1 - y * y;
    }
}
