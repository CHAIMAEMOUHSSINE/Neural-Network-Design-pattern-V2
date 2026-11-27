package chaimae.net.layer;

/** Observation locale : compte les evaluations reussies, y compris pendant train. */
public final class StatisticsLayerDecorator extends LayerDecorator {
    public record Statistics(long evaluations, double lastMin, double lastMax, double lastMean) {
    }

    private Statistics statistics = new Statistics(0, 0, 0, 0);

    public StatisticsLayerDecorator(LayerComponent component) {
        super(component);
    }

    @Override
    public double[] activate(double[] sums) {
        double[] output = super.activate(sums);
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        double scale = 0;
        for (double value : output) {
            if (!Double.isFinite(value)) {
                throw new ArithmeticException("Sortie de couche non finie.");
            }
            min = Math.min(min, value);
            max = Math.max(max, value);
            scale = Math.max(scale, Math.abs(value));
        }
        double mean = scaledMean(output, scale);
        statistics = new Statistics(statistics.evaluations() + 1, min, max, mean);
        return output;
    }

    /** Normaliser avant la somme evite le debordement de valeurs finies proches de MAX_VALUE. */
    private static double scaledMean(double[] values, double scale) {
        if (scale == 0) {
            return 0;
        }
        double normalizedSum = 0;
        for (double value : values) {
            normalizedSum += value / scale;
        }
        double normalizedMean = Math.max(-1, Math.min(1, normalizedSum / values.length));
        return scale * normalizedMean;
    }

    public Statistics statistics() { return statistics; }
}
