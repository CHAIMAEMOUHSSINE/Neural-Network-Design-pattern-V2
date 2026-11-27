package fr.enset.nn.tests;

import fr.enset.nn.activation.ActivationFactory.Type;
import fr.enset.nn.annotation.RequiresRole;
import fr.enset.nn.annotation.Role;
import fr.enset.nn.model.*;
import fr.enset.nn.security.AuthenticationService;
import fr.enset.nn.security.SecurityContext;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AspectsTest {
    private static final char[] PASSWORD = "Test-Only-2027!".toCharArray();
    private final Logger logger = Logger.getLogger("fr.enset.nn.audit");
    private final List<String> messages = new ArrayList<>();
    private final Handler capture = new Handler() {
        @Override public void publish(LogRecord record) { messages.add(record.getMessage()); }
        @Override public void flush() { }
        @Override public void close() { }
    };
    private AuthenticationService auth;
    private Level oldLevel;
    private boolean oldParents;

    @BeforeEach
    void prepare() {
        oldLevel = logger.getLevel();
        oldParents = logger.getUseParentHandlers();
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.FINE);
        capture.setLevel(Level.ALL);
        logger.addHandler(capture);
        SecurityContext.logout();
        auth = new AuthenticationService();
        auth.register("admin", PASSWORD, EnumSet.allOf(Role.class));
        auth.register("lecteur", PASSWORD, Set.of(Role.READ));
        messages.clear();
    }

    @AfterEach
    void clean() {
        SecurityContext.logout();
        logger.removeHandler(capture);
        logger.setLevel(oldLevel);
        logger.setUseParentHandlers(oldParents);
    }

    private NeuralNetwork build() {
        auth.authenticate("admin", PASSWORD);
        return new NetworkBuilder().input(1).output(1, Type.IDENTITY).build();
    }

    private List<TrainingSample> data() {
        return List.of(new TrainingSample(new double[]{1}, new double[]{0.25}));
    }

    @Test
    void toutesLesOperationsPubliquesDuReseauSontAnnotees() {
        for (var method : NeuralNetwork.class.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers()) && !method.isSynthetic()) {
                assertNotNull(method.getAnnotation(RequiresRole.class), method.getName());
            }
        }
    }

    @Test
    void utilisateurAnonymeNePeutPasConstruireNiLireLeReseau() {
        var network = new NeuralNetwork();
        assertThrows(SecurityException.class, network::state);
        assertThrows(SecurityException.class,
                () -> new NetworkBuilder().input(1).output(1, Type.IDENTITY).build());
    }

    @Test
    void administrateurExecuteEntrainementEtPrediction() {
        var network = build();
        var result = network.fit(data(), new TrainingOptions(2, 0.1));
        assertTrue(result.finalLoss() < result.initialLoss());
        assertEquals(1, network.predict(new double[]{0.8}).length);
    }

    @Test
    void lecteurNePeutNiConfigurerNiEntrainerNiPredire() {
        var network = build();
        network.fit(data(), new TrainingOptions(1, 0.1));
        auth.authenticate("lecteur", PASSWORD);
        assertEquals(fr.enset.nn.state.NetworkState.Name.TRAINED, network.state());
        assertThrows(SecurityException.class, () -> network.setWeight(0, 0.2));
        assertThrows(SecurityException.class, () -> network.fit(data(), new TrainingOptions(1, 0.1)));
        assertThrows(SecurityException.class, () -> network.train(data(), new TrainingOptions(1, 0.1)));
        assertThrows(SecurityException.class, () -> network.predict(new double[]{1}));
    }

    @Test
    void mauvaisMotDePasseEffaceLaSessionPrecedente() {
        build();
        assertThrows(SecurityException.class, () -> auth.authenticate("admin", "incorrect".toCharArray()));
        assertNull(SecurityContext.current());
        assertThrows(SecurityException.class, () -> auth.authenticate("absent", PASSWORD));
    }

    @Test
    void deconnexionEtIsolationEntreThreads() throws Exception {
        var network = build();
        AtomicReference<Throwable> error = new AtomicReference<>();
        // Évite de capturer le second thread dans une liste non synchronisée.
        logger.removeHandler(capture);
        Thread thread = new Thread(() -> {
            try { network.state(); } catch (Throwable e) { error.set(e); }
        });
        thread.start();
        thread.join();
        logger.addHandler(capture);
        assertInstanceOf(SecurityException.class, error.get());
        SecurityContext.logout();
        assertThrows(SecurityException.class, network::state);
    }

    @Test
    void tracesAvantApresDureeErreurEtAbsenceDeMotDePasse() {
        var network = build();
        network.state();
        SecurityContext.logout();
        assertThrows(SecurityException.class, network::state);
        assertTrue(messages.stream().anyMatch(s -> s.startsWith("DEBUT ") && s.contains("NeuralNetwork.state")));
        assertTrue(messages.stream().anyMatch(s -> s.contains("NeuralNetwork.state") && s.contains("statut=OK")
                && s.matches(".*duree_us=\\d+$")));
        assertTrue(messages.stream().anyMatch(s -> s.contains("NeuralNetwork.state")
                && s.contains("statut=ERREUR:SecurityException")));
        assertFalse(messages.stream().anyMatch(s -> s.contains(new String(PASSWORD))));
    }

    @Test
    void journalisationCouvreAussiLesMethodesPrivees() {
        var network = build();
        network.fit(data(), new TrainingOptions(1, 0.1));
        assertTrue(messages.stream().anyMatch(s -> s.startsWith("DEBUT ") && s.contains("NeuralNetwork.forward")));
    }
}
