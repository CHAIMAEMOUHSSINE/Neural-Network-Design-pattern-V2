package fr.enset.nn.state;

public final class TrainedState implements NetworkState {
    @Override public Name name() { return Name.TRAINED; }
    @Override public void requireConfiguration() { }
    @Override public void requireTraining() { }
    @Override public void requirePrediction() { }
}
