package ma.shopflow.catalogue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ma.shopflow.TestcontainersConfiguration;
import ma.shopflow.commande.RuptureDeStockException;

@DataJpaTest
@Import({TestcontainersConfiguration.class, StockJpaAdapter.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("Stock PostgreSQL")
class StockJpaAdapterIT {

    @Autowired
    StockJpaAdapter stock;
    @Autowired
    ProduitJpaRepository produits;

    @BeforeEach
    void remettreLeStockDeLEcranA8() {
        ProduitEntity ecran = produits.findById("ECRA-04").orElseThrow();
        ecran.setStock(8);
        produits.saveAndFlush(ecran);
    }

    @Test
    @DisplayName("réserver décrémente le stock en base")
    void reserverDecrementeLeStock() {
        stock.reserver("ECRA-04", 3);

        assertThat(stockEnBase("ECRA-04")).isEqualTo(5);
    }

    @Test
    @DisplayName("réserver au-delà du stock est refusé et ne change rien")
    void reserverAuDelaDuStockEstRefuse() {
        assertThatThrownBy(() -> stock.reserver("ECRA-04", 9))
                .isInstanceOf(RuptureDeStockException.class)
                .hasMessageContaining("ECRA-04");
        assertThat(stockEnBase("ECRA-04")).isEqualTo(8);
    }

    @Test
    @DisplayName("libérer rend le stock réservé")
    void libererRendLeStock() {
        stock.reserver("ECRA-04", 3);
        assertThat(stockEnBase("ECRA-04")).isEqualTo(5);

        stock.liberer("ECRA-04", 3);

        assertThat(stockEnBase("ECRA-04")).isEqualTo(8);
    }

    @Test
    @DisplayName("20 clients simultanés ne peuvent pas acheter plus que les 8 écrans en stock")
    void lesReservationsSimultaneesNeSurvendentPas() throws Exception {
        int clients = 20;
        CountDownLatch prets = new CountDownLatch(clients);
        CountDownLatch top = new CountDownLatch(1);
        ExecutorService guichets = Executors.newFixedThreadPool(clients);
        List<Future<Boolean>> resultats = new ArrayList<>();

        try {
            for (int i = 0; i < clients; i++) {
                resultats.add(guichets.submit(() -> {
                    prets.countDown();
                    if (!top.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Le départ des achats n'a pas été donné");
                    }
                    try {
                        stock.reserver("ECRA-04", 1);
                        return true;
                    } catch (RuptureDeStockException rupture) {
                        return false;
                    }
                }));
            }

            assertThat(prets.await(10, TimeUnit.SECONDS))
                    .as("les 20 clients sont prêts avant le départ")
                    .isTrue();
            long finAttendue = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
            top.countDown();

            int ventes = 0;
            for (Future<Boolean> resultat : resultats) {
                long attenteRestante = Math.max(1, finAttendue - System.nanoTime());
                ventes += resultat.get(attenteRestante, TimeUnit.NANOSECONDS) ? 1 : 0;
            }

            assertThat(ventes).as("nombre d'écrans vendus").isEqualTo(8);
            assertThat(stockEnBase("ECRA-04")).as("stock final").isZero();
        } finally {
            top.countDown();
            for (Future<Boolean> resultat : resultats) {
                resultat.cancel(true);
            }
            guichets.shutdownNow();
            guichets.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private int stockEnBase(String reference) {
        return produits.findById(reference).orElseThrow().getStock();
    }
}
