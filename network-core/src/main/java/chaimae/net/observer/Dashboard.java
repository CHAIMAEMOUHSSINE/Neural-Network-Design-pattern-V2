package chaimae.net.observer;

import chaimae.net.state.NetworkState.Name;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Tableau de bord textuel : historique consultable sans interface graphique. */
public final class Dashboard implements NetworkObserver {
    private final List<StateChange> events = new ArrayList<>();
    private final Map<Name, Long> counts = new EnumMap<>(Name.class);

    public Dashboard() {
        for (Name name : Name.values()) {
            counts.put(name, 0L);
        }
    }

    @Override
    public void onStateChange(StateChange change) {
        Objects.requireNonNull(change, "Le changement d'etat est requis.");
        Name next = Objects.requireNonNull(change.to(), "Le nouvel etat est requis.");
        events.add(change);
        counts.merge(next, 1L, Long::sum);
    }

    public List<StateChange> events() {
        return List.copyOf(events);
    }

    /** Nombre d'entrées observées dans chaque état, pas le nombre d'epochs. */
    public Map<Name, Long> statistics() {
        return Map.copyOf(counts);
    }
}
