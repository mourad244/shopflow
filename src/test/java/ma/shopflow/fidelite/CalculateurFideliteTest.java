package ma.shopflow.fidelite;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Points de fidélité")
class CalculateurFideliteTest {

    private final CalculateurFidelite calculateur = new CalculateurFidelite();

    @Test
    @DisplayName("F1 : 100 DH rapportent 10 points")
    void centDirhamsRapportentDixPoints() {
        assertThat(calculateur.points(new BigDecimal("100.00"))).isEqualTo(10);
    }
}
