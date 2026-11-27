package fr.enset.nn.state;

public interface NetworkState {
    enum Name { CONSTRUCTION, TRAINING, TRAINED }

    Name name();

    void requireConfiguration();

    void requireTraining();

    void requirePrediction();
}
