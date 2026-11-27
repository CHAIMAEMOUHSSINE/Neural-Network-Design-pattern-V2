package chaimae.net.aop;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclarePrecedence;

@Aspect
@DeclarePrecedence("chaimae.net.aop.LoggingAspect, chaimae.net.aop.SecurityAspect")
public class LoggingAspect {
    private static final Logger LOGGER = Logger.getLogger("chaimae.net.audit");

    @Around("execution(* chaimae.net..*(..)) && !within(chaimae.net.aop..*)"
            + " && !within(chaimae.net.tests..*)")
    public Object log(ProceedingJoinPoint point) throws Throwable {
        if (!LOGGER.isLoggable(Level.FINE)) {
            return point.proceed();
        }
        String signature = point.getSignature().toLongString();
        long start = System.nanoTime();
        String status = "OK";
        LOGGER.fine("DEBUT " + signature);
        try {
            return point.proceed();
        } catch (Throwable error) {
            status = "ERREUR:" + error.getClass().getSimpleName();
            throw error;
        } finally {
            LOGGER.fine("FIN " + signature + " statut=" + status
                    + " duree_us=" + (System.nanoTime() - start) / 1_000);
        }
    }
}
