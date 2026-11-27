package chaimae.net.activation;

/** Contrat historique scalaire, different du contrat vectoriel des neurones. */
public interface LegacyActivation {
    double calculer(double valeur);
    double derivee(double valeur);
}
