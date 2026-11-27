package fr.enset.nn.observer;

@FunctionalInterface
public interface NetworkObserver {
    void onStateChange(StateChange change);
}
