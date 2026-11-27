package fr.enset.nn.activation;

public final class Softmax implements ActivationFunction {
    @Override
    public double activate(double[] sums, int index) {
        double max = Double.NEGATIVE_INFINITY;
        for (double sum : sums) {
            max = Math.max(max, sum);
        }
        double denominator = 0;
        for (double sum : sums) {
            denominator += Math.exp(sum - max);
        }
        return Math.exp(sums[index] - max) / denominator;
    }

    @Override
    public double derivative(double[] sums, int outputIndex, int inputIndex) {
        double yi = activate(sums, outputIndex);
        double yj = activate(sums, inputIndex);
        return yi * ((outputIndex == inputIndex ? 1 : 0) - yj);
    }
}
