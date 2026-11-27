package chaimae.net.demo;

import chaimae.net.activation.ActivationFactory.Type;
import chaimae.net.model.NetworkBuilder;
import chaimae.net.model.NeuralNetwork;
import chaimae.net.model.TrainingOptions;
import chaimae.net.model.TrainingSample;
import chaimae.net.observer.Dashboard;
import chaimae.net.layer.StatisticsLayerDecorator;
import chaimae.net.layer.ScalingLayerDecorator;
import java.util.Arrays;
import java.util.List;

public final class DemoModele {
    private DemoModele() {
    }

    public static void main(String[] args) {
        NeuralNetwork figure = new NetworkBuilder().input(3).hidden(5, Type.SIGMOID)
                .output(2, Type.SOFTMAX).seed(42).build();
        System.out.println("Architecture 3-5-2 : " + figure.connections().size() + " connexions");
        figure.decorateLayer(1, layer -> new ScalingLayerDecorator(layer, 1.1));
        figure.decorateLayer(1, StatisticsLayerDecorator::new);
        figure.fit(List.of(new TrainingSample(new double[]{0.2, 0.4, 0.6}, new double[]{1, 0})),
                new TrainingOptions(10, 0.05));
        System.out.println("Sortie Softmax : " + Arrays.toString(figure.predict(new double[]{0.2, 0.4, 0.6})));
        System.out.println("Couche cachee decoree : "
                + ((StatisticsLayerDecorator) figure.layers().get(1)).statistics());

        NeuralNetwork linear = new NetworkBuilder().input(1).output(1, Type.IDENTITY).seed(7).build();
        Dashboard dashboard = new Dashboard();
        linear.subscribe(dashboard);
        var data = List.of(
                new TrainingSample(new double[]{0}, new double[]{-0.4}),
                new TrainingSample(new double[]{1}, new double[]{0.2}),
                new TrainingSample(new double[]{2}, new double[]{0.8}));
        var result = linear.train(data, new TrainingOptions(1000, 0.05));
        System.out.println("Apprentissage reel : " + result);
        System.out.println("Poids appris : " + linear.connections().getFirst().weight());
        System.out.println("Biais appris : " + linear.layers().getLast().neurons().getFirst().bias());
        System.out.println("Prediction [1.5] : " + Arrays.toString(linear.predict(new double[]{1.5})));
        System.out.println("Notifications : " + dashboard.events());
        System.out.println("Statistiques : " + dashboard.statistics());
    }
}
