package chaimae.net.aop;

import chaimae.net.annotation.RequiresRole;
import chaimae.net.security.SecurityContext;
import chaimae.net.security.User;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

@Aspect
public class SecurityAspect {
    @Before("execution(@chaimae.net.annotation.RequiresRole * *(..)) && @annotation(requirement)")
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
