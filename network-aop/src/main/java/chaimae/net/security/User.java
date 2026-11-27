package chaimae.net.security;

import chaimae.net.annotation.Role;
import java.util.Objects;
import java.util.Set;

public record User(String username, Set<Role> roles) {
    public User {
        Objects.requireNonNull(username, "L'identifiant est requis.");
        if (username.isBlank()) {
            throw new IllegalArgumentException("L'identifiant ne doit pas etre vide.");
        }
        roles = Set.copyOf(Objects.requireNonNull(roles, "Les roles sont requis."));
    }
}
