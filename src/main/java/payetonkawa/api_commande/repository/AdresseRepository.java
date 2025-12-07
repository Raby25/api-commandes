package payetonkawa.api_commande.repository;

import payetonkawa.api_commande.model.Adresse;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdresseRepository extends JpaRepository<Adresse, Long> {
    Optional<Adresse> findByNumeroRueAndRueAndVilleAndCodePostalAndPays(
        int numeroRue,
        String rue,
        String ville,
        String codePostal,
        String pays);
}
