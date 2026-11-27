package fr.enset.nn.state;

public final class TrainingState implements NetworkState {
    @Override public Name name() { return Name.TRAINING; }
    @Override public void requireConfiguration() { refuse(); }
    @Override public void requireTraining() { refuse(); }
    @Override public void requirePrediction() { refuse(); }
    private void refuse() {
        throw new IllegalStateException("Operation interdite pendant l'entrainement.");
    }
}
