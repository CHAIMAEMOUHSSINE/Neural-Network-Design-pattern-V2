package chaimae.net.observer;

import chaimae.net.state.NetworkState;

public record StateChange(NetworkState.Name from, NetworkState.Name to, String reason) {
}
