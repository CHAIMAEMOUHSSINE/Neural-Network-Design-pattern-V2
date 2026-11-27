package chaimae.net.tests;

import chaimae.net.activation.ActivationFactory.Type;
import chaimae.net.annotation.RequiresRole;
import chaimae.net.annotation.Role;
import chaimae.net.model.*;
import chaimae.net.layer.StatisticsLayerDecorator;
import chaimae.net.security.AuthenticationService;
import chaimae.net.security.SecurityContext;
import chaimae.net.security.User;
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
    @Test
    void inscriptionInvalideNeReservePasUnIdentifiant() {
        assertThrows(IllegalArgumentException.class,
                () -> auth.register("admin", PASSWORD, Set.of(Role.READ)));
        assertThrows(IllegalArgumentException.class,
                () -> auth.register("nouveau", "court".toCharArray(), Set.of(Role.READ)));
        assertThrows(NullPointerException.class,
                () -> auth.register("nouveau", PASSWORD, null));
        auth.register("nouveau", PASSWORD, Set.of(Role.READ));
        assertEquals(Set.of(Role.READ), auth.authenticate("nouveau", PASSWORD).roles());
    }

    @Test
    void identiteValideSonContratEtCopieLesRoles() {
        var roles = EnumSet.of(Role.READ);
        var user = new User("lecteur", roles);
        roles.add(Role.TRAIN);
        assertEquals(Set.of(Role.READ), user.roles());
        assertThrows(UnsupportedOperationException.class, () -> user.roles().add(Role.PREDICT));
        assertThrows(NullPointerException.class, () -> new User(null, roles));
        assertThrows(IllegalArgumentException.class, () -> new User("   ", roles));
        assertThrows(NullPointerException.class, () -> new User("lecteur", null));
    }

    @Test
    void auditDesactiveConserveResultatsEtExceptionsSansProduireDeMessages() {
        var network = build();
        messages.clear();
        logger.setLevel(Level.OFF);
        assertEquals(chaimae.net.state.NetworkState.Name.CONSTRUCTION, network.state());
        assertThrows(IllegalStateException.class, () -> network.predict(new double[]{1}));
        assertTrue(messages.isEmpty());
        logger.setLevel(Level.FINE);
        network.state();
        assertTrue(messages.stream().anyMatch(s -> s.contains("NeuralNetwork.state") && s.contains("statut=OK")));
    }

    private static final char[] PASSWORD = "Test-Only-2027!".toCharArray();
    private final Logger logger = Logger.getLogger("chaimae.net.audit");
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
        assertEquals(chaimae.net.state.NetworkState.Name.TRAINED, network.state());
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

    @Test
    void nouveauxPointsEntreeV2SontProtegesParLeTissage() {
        var network = build();
        network.setBias(1, 0, -0.2);
        network.decorateLayer(1, StatisticsLayerDecorator::new);
        auth.authenticate("lecteur", PASSWORD);
        assertThrows(SecurityException.class, () -> network.setBias(1, 0, 0.5));
        assertThrows(SecurityException.class, () -> network.decorateLayer(1, StatisticsLayerDecorator::new));
        assertThrows(SecurityException.class, () -> network.undecorateLayer(1));
        assertEquals(-0.2, network.layers().get(1).neurons().getFirst().bias());
    }

    @Test
    void auditCouvreLesDecorateursV2EtLesMutationsDeBiais() {
        var network = build();
        network.setBias(1, 0, 0.1);
        network.decorateLayer(1, StatisticsLayerDecorator::new);
        network.fit(data(), new TrainingOptions(1, 0.1));
        assertTrue(messages.stream().anyMatch(s -> s.contains("StatisticsLayerDecorator.activate")));
        assertTrue(messages.stream().anyMatch(s -> s.contains("NeuralNetwork.setBias") && s.contains("statut=OK")));
    }
}
