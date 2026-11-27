package chaimae.net.state;

public interface NetworkState {
    enum Name { CONSTRUCTION, TRAINING, TRAINED }

    Name name();

    void requireConfiguration();

    void requireTraining();

    void requirePrediction();
}
