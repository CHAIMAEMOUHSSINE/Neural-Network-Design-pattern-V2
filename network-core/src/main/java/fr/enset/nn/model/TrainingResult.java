package fr.enset.nn.model;

public record TrainingResult(double initialLoss, double finalLoss, int epochs) {
}
