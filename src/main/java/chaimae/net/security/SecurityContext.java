package chaimae.net.security;

public final class SecurityContext {
    private static final ThreadLocal<User> CURRENT = new ThreadLocal<>();

    private SecurityContext() {
    }

    static void login(User user) { CURRENT.set(user); }
    public static User current() { return CURRENT.get(); }
    public static void logout() { CURRENT.remove(); }
}
