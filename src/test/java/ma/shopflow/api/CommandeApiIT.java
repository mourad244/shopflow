package ma.shopflow.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import ma.shopflow.TestcontainersConfiguration;
import ma.shopflow.commande.Commande;
import ma.shopflow.commande.NotificationService;
import ma.shopflow.commande.StatutCommande;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@DisplayName("API des commandes")
class CommandeApiIT {

    @Autowired
    MockMvcTester mvc;
    @Autowired
    JdbcTemplate jdbc;
    @MockitoBean
    NotificationService notifications;

    @BeforeEach
    void remettreLeCatalogueEnEtat() {
        jdbc.update("delete from ligne_commande");
        jdbc.update("delete from commande");
        jdbc.update("update produit set stock = 40 where reference = 'SOUR-02'");
        jdbc.update("update produit set stock = 5 where reference = 'PORT-06'");
        jdbc.update("update produit set stock = 0 where reference = 'CASQ-05'");
    }

    @Test
    @DisplayName("une commande valide est créée, enregistrée en base, et le stock diminue")
    void uneCommandeValideEstCreee() {
        var reponse = mvc.post().uri("/api/commandes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"clientId": "C-001", "email": "amina@exemple.ma",
                         "lignes": [{"reference": "SOUR-02", "quantite": 2}]}
                        """)
                .exchange();

        assertThat(reponse).hasStatus(HttpStatus.CREATED)
                .bodyJson().extractingPath("$.total").isEqualTo(270.00);
        assertThat(reponse).bodyJson().extractingPath("$.statut").isEqualTo("CONFIRMEE");
        assertStocks(38, 5, 0);
        assertThat(nombreCommandes()).isEqualTo(1);
        assertThat(nombreLignes()).isEqualTo(1);

        ArgumentCaptor<Commande> captor = ArgumentCaptor.forClass(Commande.class);
        verify(notifications).envoyerConfirmation(eq("amina@exemple.ma"), captor.capture());
        Commande notifiee = captor.getValue();
        assertThat(notifiee.clientId()).isEqualTo("C-001");
        assertThat(notifiee.statut()).isEqualTo(StatutCommande.CONFIRMEE);
        assertThat(notifiee.total()).isEqualByComparingTo("270.00");
        assertThat(notifiee.referencePaiement()).startsWith("PAY-");
        assertThat(notifiee.lignes()).hasSize(1);
        assertThat(notifiee.lignes().getFirst().produit().reference()).isEqualTo("SOUR-02");
        assertThat(notifiee.lignes().getFirst().quantite()).isEqualTo(2);
        assertThat(reponse).bodyJson().extractingPath("$.id").isEqualTo(notifiee.id());
        assertThat(reponse).bodyJson().extractingPath("$.referencePaiement")
                .isEqualTo(notifiee.referencePaiement());

        assertThat(jdbc.queryForObject("select client_id from commande where id = ?", String.class, notifiee.id()))
                .isEqualTo("C-001");
        assertThat(jdbc.queryForObject("select statut from commande where id = ?", String.class, notifiee.id()))
                .isEqualTo("CONFIRMEE");
        assertThat(jdbc.queryForObject("select total from commande where id = ?", BigDecimal.class, notifiee.id()))
                .isEqualByComparingTo("270.00");
        assertThat(jdbc.queryForObject("select reference_paiement from commande where id = ?", String.class,
                notifiee.id())).isEqualTo(notifiee.referencePaiement());
        assertThat(jdbc.queryForObject("select reference from ligne_commande where commande_id = ?", String.class,
                notifiee.id())).isEqualTo("SOUR-02");
        assertThat(jdbc.queryForObject("select quantite from ligne_commande where commande_id = ?", Integer.class,
                notifiee.id())).isEqualTo(2);
        assertThat(jdbc.queryForObject("select prix_unitaire from ligne_commande where commande_id = ?",
                BigDecimal.class, notifiee.id())).isEqualByComparingTo("120.00");
    }

    @Test
    @DisplayName("un produit en rupture donne 409 Conflict")
    void unProduitEnRuptureDonne409() {
        var reponse = mvc.post().uri("/api/commandes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"clientId": "C-001", "email": "amina@exemple.ma",
                         "lignes": [{"reference": "CASQ-05", "quantite": 1}]}
                        """)
                .exchange();

        assertThat(reponse).hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.detail").isEqualTo("Produit indisponible : CASQ-05");
        assertThat(reponse).bodyJson().extractingPath("$.status").isEqualTo(409);
        assertStocks(40, 5, 0);
        assertAucuneCommandeEnregistreeNiNotifiee();
    }

    @Test
    @DisplayName("un paiement refusé donne 402 et rend le stock réservé")
    void unPaiementRefuseRendLeStock() {
        var reponse = mvc.post().uri("/api/commandes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"clientId": "C-001", "email": "amina@exemple.ma",
                         "lignes": [{"reference": "PORT-06", "quantite": 2}]}
                        """)
                .exchange();

        assertThat(reponse).hasStatus(HttpStatus.PAYMENT_REQUIRED)
                .bodyJson().extractingPath("$.detail").isEqualTo("Paiement refusé : Plafond de la carte dépassé");
        assertThat(reponse).bodyJson().extractingPath("$.status").isEqualTo(402);
        assertStocks(40, 5, 0);
        assertAucuneCommandeEnregistreeNiNotifiee();
    }

    @Test
    @DisplayName("une demande invalide donne 400 Bad Request")
    void uneDemandeInvalideDonne400() {
        var reponse = mvc.post().uri("/api/commandes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"clientId": "C-001", "email": "pas-un-email",
                         "lignes": [{"reference": "SOUR-02", "quantite": 0}]}
                        """)
                .exchange();

        assertThat(reponse).hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.status").isEqualTo(400);
        assertThat(reponse).bodyJson().extractingPath("$.title").isEqualTo("Bad Request");
        assertStocks(40, 5, 0);
        assertAucuneCommandeEnregistreeNiNotifiee();
    }

    private void assertStocks(int souris, int portable, int casque) {
        assertThat(stock("SOUR-02")).as("stock des souris").isEqualTo(souris);
        assertThat(stock("PORT-06")).as("stock des portables").isEqualTo(portable);
        assertThat(stock("CASQ-05")).as("stock des casques").isEqualTo(casque);
    }

    private void assertAucuneCommandeEnregistreeNiNotifiee() {
        assertThat(nombreCommandes()).isZero();
        assertThat(nombreLignes()).isZero();
        verifyNoInteractions(notifications);
    }

    private int nombreCommandes() {
        return jdbc.queryForObject("select count(*) from commande", Integer.class);
    }

    private int nombreLignes() {
        return jdbc.queryForObject("select count(*) from ligne_commande", Integer.class);
    }

    private int stock(String reference) {
        return jdbc.queryForObject("select stock from produit where reference = ?", Integer.class, reference);
    }
}
