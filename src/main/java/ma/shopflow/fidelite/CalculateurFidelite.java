package ma.shopflow.fidelite;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calcule les points de fidélité gagnés par une commande.
 */
public class CalculateurFidelite {

    private static final BigDecimal MONTANT_PAR_POINT = BigDecimal.TEN;

    public int points(BigDecimal montant) {
        return points(montant, NiveauFidelite.STANDARD);
    }

    public int points(BigDecimal montant, NiveauFidelite niveau) {
        return pointsDeBase(montant) * niveau.multiplicateur();
    }

    private int pointsDeBase(BigDecimal montant) {
        return montant.divide(MONTANT_PAR_POINT, 0, RoundingMode.DOWN).intValueExact();
    }
}
