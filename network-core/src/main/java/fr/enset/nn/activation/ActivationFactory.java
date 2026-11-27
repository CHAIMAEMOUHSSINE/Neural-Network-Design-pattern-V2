package fr.enset.nn.activation;

/** Simple Factory, et non Factory Method par héritage. */
public final class ActivationFactory {
    public enum Type { IDENTITY, SIGMOID, SOFTMAX, LEGACY_TANH }

    private ActivationFactory() {
    }

    public static ActivationFunction create(Type type) {
        return switch (type) {
            case IDENTITY -> new Identity();
            case SIGMOID -> new Sigmoid();
            case SOFTMAX -> new Softmax();
            case LEGACY_TANH -> new LegacyActivationAdapter(new AncienneFonctionTanh());
        };
    }
}
