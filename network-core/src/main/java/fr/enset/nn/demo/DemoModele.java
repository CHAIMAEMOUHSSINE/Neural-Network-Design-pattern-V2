package fr.enset.nn.demo;

import fr.enset.nn.activation.ActivationFactory.Type;
import fr.enset.nn.model.NetworkBuilder;
import fr.enset.nn.model.NeuralNetwork;
import fr.enset.nn.model.TrainingOptions;
import fr.enset.nn.model.TrainingSample;
import fr.enset.nn.observer.Dashboard;
import java.util.Arrays;
import java.util.List;

public final class DemoModele {
    private DemoModele() {
    }

    public static void main(String[] args) {
        NeuralNetwork figure = new NetworkBuilder().input(3).hidden(5, Type.SIGMOID)
                .output(2, Type.SOFTMAX).seed(42).build();
        System.out.println("Architecture 3-5-2 : " + figure.connections().size() + " connexions");
        figure.fit(List.of(new TrainingSample(new double[]{0.2, 0.4, 0.6}, new double[]{1, 0})),
                new TrainingOptions(10, 0.05));
        System.out.println("Sortie Softmax : " + Arrays.toString(figure.predict(new double[]{0.2, 0.4, 0.6})));

        NeuralNetwork linear = new NetworkBuilder().input(2).output(1, Type.IDENTITY).seed(7).build();
        Dashboard dashboard = new Dashboard();
        linear.subscribe(dashboard);
        var data = List.of(
                new TrainingSample(new double[]{1, 0}, new double[]{0.25}),
                new TrainingSample(new double[]{0, 1}, new double[]{0.75}),
                new TrainingSample(new double[]{1, 1}, new double[]{1}));
        var result = linear.train(data, new TrainingOptions(200, 0.1));
        System.out.println("Apprentissage reel : " + result);
        System.out.println("Prediction [0.4, 0.8] : " + Arrays.toString(linear.predict(new double[]{0.4, 0.8})));
        System.out.println("Notifications : " + dashboard.events());
        System.out.println("Statistiques : " + dashboard.statistics());
    }
}
