package chaimae.net.tests;

import chaimae.net.annotation.Role;
import chaimae.net.security.AuthenticationService;
import chaimae.net.security.SecurityContext;
import java.util.Arrays;
import java.util.EnumSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/** Les tests du modele utilisent les memes classes tissees que l'application. */
abstract class AuthenticatedNetworkTest {
    private static final AuthenticationService ACCOUNTS = createAccounts();

    private static AuthenticationService createAccounts() {
        var accounts = new AuthenticationService();
        char[] password = "Model-Test-2027!".toCharArray();
        try {
            accounts.register("test-admin", password, EnumSet.allOf(Role.class));
            return accounts;
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    @BeforeEach
    void authenticateModelTest() {
        char[] password = "Model-Test-2027!".toCharArray();
        try {
            ACCOUNTS.authenticate("test-admin", password);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    @AfterEach
    void clearModelTestIdentity() {
        SecurityContext.logout();
    }
}
