package chaimae.net.model;

import java.util.Objects;

public record TrainingSample(double[] input, double[] target) {
    public TrainingSample {
        input = Objects.requireNonNull(input, "Le vecteur d'entree est requis.").clone();
        target = Objects.requireNonNull(target, "Le vecteur cible est requis.").clone();
    }

    @Override
    public double[] input() {
        return input.clone();
    }

    @Override
    public double[] target() {
        return target.clone();
    }
}
