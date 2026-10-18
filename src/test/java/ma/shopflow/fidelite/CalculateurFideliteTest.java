package ma.shopflow.fidelite;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Points de fidélité")
class CalculateurFideliteTest {

    private final CalculateurFidelite calculateur = new CalculateurFidelite();

    @Test
    @DisplayName("F1 : 100 DH rapportent 10 points")
    void centDirhamsRapportentDixPoints() {
        assertThat(calculateur.points(new BigDecimal("100.00"))).isEqualTo(10);
    }

    @ParameterizedTest(name = "{0} DH -> {1} points")
    @CsvSource({
            "0.00,    0",
            "9.99,    0",
            "10.00,   1",
            "99.99,   9",
            "250.00, 25"
    })
    @DisplayName("F1 : un point par tranche entière de 10 DH")
    void unPointParTrancheEntiereDeDixDirhams(String montant, int pointsAttendus) {
        assertThat(calculateur.points(new BigDecimal(montant))).isEqualTo(pointsAttendus);
    }

    @Test
    @DisplayName("F2 : un client GOLD gagne deux fois plus de points")
    void unClientGoldGagneDeuxFoisPlusDePoints() {
        assertThat(calculateur.points(new BigDecimal("250.00"), NiveauFidelite.GOLD)).isEqualTo(50);
    }
}
