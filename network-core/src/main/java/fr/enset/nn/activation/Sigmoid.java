package fr.enset.nn.activation;

public final class Sigmoid implements ActivationFunction {
    @Override
    public double activate(double[] sums, int index) {
        double x = sums[index];
        if (x >= 0) {
            return 1 / (1 + Math.exp(-x));
        }
        double exponential = Math.exp(x);
        return exponential / (1 + exponential);
    }

    @Override
    public double derivative(double[] sums, int outputIndex, int inputIndex) {
        if (outputIndex != inputIndex) {
            return 0;
        }
        double y = activate(sums, outputIndex);
        return y * (1 - y);
    }
}
