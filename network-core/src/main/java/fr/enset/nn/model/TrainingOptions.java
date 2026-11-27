package fr.enset.nn.model;

public record TrainingOptions(int epochs, double learningRate) {
    public TrainingOptions {
        if (epochs < 1 || !Double.isFinite(learningRate) || learningRate <= 0) {
            throw new IllegalArgumentException("Epochs >= 1 et taux d'apprentissage positif fini requis.");
        }
    }
}
