package payetonkawa.api_commande.repository;

import payetonkawa.api_commande.model.Commande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommandeRepository extends JpaRepository<Commande, Long> {

    // rechercher une commande par son numéro unique
    Optional<Commande> findByNumCommande(String numCommande);

    List<Commande> findByIdClient(Long idClient);
}
