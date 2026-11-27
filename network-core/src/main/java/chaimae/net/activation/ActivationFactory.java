package chaimae.net.activation;

import java.util.Objects;

/** Simple Factory, et non Factory Method par héritage. */
public final class ActivationFactory {
    public enum Type { IDENTITY, SIGMOID, SOFTMAX, LEGACY_TANH }

    private ActivationFactory() {
    }

    public static ActivationFunction create(Type type) {
        Objects.requireNonNull(type, "Le type d'activation est requis.");
        return switch (type) {
            case IDENTITY -> new Identity();
            case SIGMOID -> new Sigmoid();
            case SOFTMAX -> new Softmax();
            case LEGACY_TANH -> new LegacyActivationAdapter(new AncienneFonctionTanh());
        };
    }
}
