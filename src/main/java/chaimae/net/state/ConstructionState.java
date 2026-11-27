package chaimae.net.state;

public final class ConstructionState implements NetworkState {
    @Override public Name name() { return Name.CONSTRUCTION; }
    @Override public void requireConfiguration() { }
    @Override public void requireTraining() { }
    @Override public void requirePrediction() {
        throw new IllegalStateException("Le reseau doit etre entraine avant predict.");
    }
}
