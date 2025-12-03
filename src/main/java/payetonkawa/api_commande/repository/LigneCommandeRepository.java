package payetonkawa.api_commande.repository;

import payetonkawa.api_commande.model.LigneCommande;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LigneCommandeRepository extends JpaRepository<LigneCommande, Long> {
    // Exemples de méthodes personnalisées possibles :
    List<LigneCommande> findByCommandeId(Long commandeId);
}
