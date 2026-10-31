package ma.shopflow.catalogue;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ma.shopflow.commande.RuptureDeStockException;
import ma.shopflow.commande.StockService;

/** Implémentation du stock adossée à la table produit. */
@Component
@Transactional
public class StockJpaAdapter implements StockService {

    private final ProduitJpaRepository produits;

    public StockJpaAdapter(ProduitJpaRepository produits) {
        this.produits = produits;
    }

    @Override
    public boolean estDisponible(String reference, int quantite) {
        return produits.findById(reference)
                .map(produit -> produit.getStock() >= quantite)
                .orElse(false);
    }

    @Override
    public void reserver(String reference, int quantite) {
        ProduitEntity produit = produits.findById(reference)
                .orElseThrow(() -> new ProduitInconnuException(reference));
        if (produit.getStock() < quantite) {
            throw new RuptureDeStockException(reference);
        }
        produit.setStock(produit.getStock() - quantite);
        produits.save(produit);
    }

    @Override
    public void liberer(String reference, int quantite) {
        ProduitEntity produit = produits.findById(reference)
                .orElseThrow(() -> new ProduitInconnuException(reference));
        produit.setStock(produit.getStock() + quantite);
        produits.save(produit);
    }
}
