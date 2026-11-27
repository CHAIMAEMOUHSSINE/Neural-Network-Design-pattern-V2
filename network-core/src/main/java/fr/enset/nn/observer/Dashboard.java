package fr.enset.nn.observer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.EnumMap;
import fr.enset.nn.state.NetworkState.Name;

/** Tableau de bord textuel : historique consultable sans interface graphique. */
public final class Dashboard implements NetworkObserver {
    private final List<StateChange> events = new ArrayList<>();

    @Override
    public void onStateChange(StateChange change) {
        events.add(change);
    }

    public List<StateChange> events() {
        return List.copyOf(events);
    }

    /** Nombre d'entrées observées dans chaque état, pas le nombre d'epochs. */
    public Map<Name, Long> statistics() {
        Map<Name, Long> counts = new EnumMap<>(Name.class);
        for (Name name : Name.values()) {
            counts.put(name, 0L);
        }
        events.forEach(event -> counts.merge(event.to(), 1L, Long::sum));
        return Map.copyOf(counts);
    }
}
