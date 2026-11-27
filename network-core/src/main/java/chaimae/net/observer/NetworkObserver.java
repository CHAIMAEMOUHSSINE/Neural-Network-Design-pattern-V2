package chaimae.net.observer;

@FunctionalInterface
public interface NetworkObserver {
    void onStateChange(StateChange change);
}
