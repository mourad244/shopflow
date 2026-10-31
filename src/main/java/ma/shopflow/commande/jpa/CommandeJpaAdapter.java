package ma.shopflow.commande.jpa;

import org.springframework.stereotype.Component;

import ma.shopflow.commande.Commande;
import ma.shopflow.commande.CommandeRepository;

/** Stocke une commande et ses lignes dans les tables commande et ligne_commande. */
@Component
public class CommandeJpaAdapter implements CommandeRepository {

    private final CommandeJpaRepository jpa;

    public CommandeJpaAdapter(CommandeJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Commande enregistrer(Commande commande) {
        jpa.save(new CommandeEntity(commande.id(), commande.clientId(), commande.total(),
                commande.referencePaiement(), commande.statut(),
                commande.lignes().stream()
                        .map(ligne -> new LigneCommandeEmbeddable(ligne.produit().reference(),
                                ligne.quantite(), ligne.produit().prixUnitaire()))
                        .toList()));
        return commande;
    }
}
