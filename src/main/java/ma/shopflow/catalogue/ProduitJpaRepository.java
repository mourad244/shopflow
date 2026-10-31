package ma.shopflow.catalogue;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
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
}
