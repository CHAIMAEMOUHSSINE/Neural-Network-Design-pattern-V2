package chaimae.net.security;

import chaimae.net.annotation.Role;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Comptes en mémoire. Inscription réservée au code de bootstrap de confiance. */
public final class AuthenticationService {
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final int HASH_ITERATIONS = 210_000;
    private static final int MIN_PASSWORD_LENGTH = 8;

    private record Account(User user, byte[] salt, byte[] hash) {
    }

    private final Map<String, Account> accounts = new HashMap<>();
    private final SecureRandom random = new SecureRandom();

    public void register(String username, char[] password, Set<Role> roles) {
        Objects.requireNonNull(username);
        Objects.requireNonNull(password);
        if (username.isBlank() || password.length < MIN_PASSWORD_LENGTH || accounts.containsKey(username)) {
            throw new IllegalArgumentException("Identifiant unique et mot de passe d'au moins 8 caracteres requis.");
        }
        User user = new User(username, roles);
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        accounts.put(username, new Account(user, salt, derive(password, salt)));
    }

    public User authenticate(String username, char[] password) {
        SecurityContext.logout();
        Objects.requireNonNull(password);
        Account account = accounts.get(username);
        byte[] candidate = derive(password, account == null ? new byte[SALT_BYTES] : account.salt());
        try {
            if (account == null || !MessageDigest.isEqual(account.hash(), candidate)) {
                throw new SecurityException("Identifiants incorrects.");
            }
            SecurityContext.login(account.user());
            return account.user();
        } finally {
            Arrays.fill(candidate, (byte) 0);
        }
    }

    private static byte[] derive(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, HASH_ITERATIONS, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 indisponible.", e);
        } finally {
            spec.clearPassword();
        }
    }
}
