package ma.shopflow.catalogue;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProduitJpaRepository extends JpaRepository<ProduitEntity, String> {

    /** Recherche PostgreSQL insensible à la casse et aux accents. */
    @Query(value = """
            select * from produit
            where unaccent(lower(nom)) like unaccent(lower('%' || :texte || '%'))
            order by nom
            """, nativeQuery = true)
    List<ProduitEntity> rechercher(@Param("texte") String texte);

    /** Réserve seulement si le stock suffit ; renvoie le nombre de lignes modifiées. */
    @Modifying
    @Query("update ProduitEntity p set p.stock = p.stock - :quantite "
            + "where p.reference = :reference and p.stock >= :quantite")
    int decrementerStock(@Param("reference") String reference, @Param("quantite") int quantite);

    /** Restitue la quantité sans écraser une modification concurrente. */
    @Modifying
    @Query("update ProduitEntity p set p.stock = p.stock + :quantite where p.reference = :reference")
    int incrementerStock(@Param("reference") String reference, @Param("quantite") int quantite);
}
