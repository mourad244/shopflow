package ma.shopflow.fidelite;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calcule les points de fidélité gagnés par une commande.
 */
public class CalculateurFidelite {

    public int points(BigDecimal montant) {
        return montant.divide(BigDecimal.TEN, 0, RoundingMode.DOWN).intValueExact();
    }
}
