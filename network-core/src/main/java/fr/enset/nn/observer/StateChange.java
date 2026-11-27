package fr.enset.nn.observer;

import fr.enset.nn.state.NetworkState;

public record StateChange(NetworkState.Name from, NetworkState.Name to, String reason) {
}
