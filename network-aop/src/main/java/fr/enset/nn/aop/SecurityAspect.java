package fr.enset.nn.aop;

import fr.enset.nn.annotation.RequiresRole;
import fr.enset.nn.security.SecurityContext;
import fr.enset.nn.security.User;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

@Aspect
public class SecurityAspect {
    @Before("execution(@fr.enset.nn.annotation.RequiresRole * *(..)) && @annotation(requirement)")
    public void verify(RequiresRole requirement) {
        User user = SecurityContext.current();
        if (user == null) {
            throw new SecurityException("Authentification requise.");
        }
        if (!user.roles().contains(requirement.value())) {
            throw new SecurityException("Role requis : " + requirement.value());
        }
    }
}
