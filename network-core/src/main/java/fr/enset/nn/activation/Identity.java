package fr.enset.nn.activation;

public final class Identity implements ActivationFunction {
    @Override
    public double activate(double[] sums, int index) {
        return sums[index];
    }

    @Override
    public double derivative(double[] sums, int outputIndex, int inputIndex) {
        return outputIndex == inputIndex ? 1 : 0;
    }
}
