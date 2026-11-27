package fr.enset.nn.model;

public record TrainingSample(double[] input, double[] target) {
    public TrainingSample {
        input = input.clone();
        target = target.clone();
    }

    @Override public double[] input() { return input.clone(); }
    @Override public double[] target() { return target.clone(); }
}
