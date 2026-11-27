package fr.enset.nn.security;

import fr.enset.nn.annotation.Role;
import java.util.Set;

public record User(String username, Set<Role> roles) {
    public User {
        roles = Set.copyOf(roles);
    }
}
