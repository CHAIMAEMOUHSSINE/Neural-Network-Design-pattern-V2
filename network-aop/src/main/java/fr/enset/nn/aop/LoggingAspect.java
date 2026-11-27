package fr.enset.nn.aop;

import java.util.logging.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclarePrecedence;

@Aspect
@DeclarePrecedence("fr.enset.nn.aop.LoggingAspect, fr.enset.nn.aop.SecurityAspect")
public class LoggingAspect {
    private static final Logger LOGGER = Logger.getLogger("fr.enset.nn.audit");

    @Around("execution(* fr.enset.nn..*(..)) && !within(fr.enset.nn.aop..*)"
            + " && !within(fr.enset.nn.tests..*)")
    public Object log(ProceedingJoinPoint point) throws Throwable {
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
