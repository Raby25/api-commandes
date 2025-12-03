package payetonkawa.api_commande.services;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import payetonkawa.api_commande.dto.AdresseDto;
import payetonkawa.api_commande.dto.CommandeDto;
import payetonkawa.api_commande.dto.LigneCommandeDto;
import payetonkawa.api_commande.model.Adresse;
import payetonkawa.api_commande.model.Commande;
import payetonkawa.api_commande.repository.CommandeRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommandeService {

    private final CommandeRepository repository;
    private final RabbitTemplate rabbitTemplate;

    private final String exchange = "commandes.exchange";

    public Commande create(Commande c) {
        Commande saved = repository.save(c);

        // Mapper vers DTO RabbitMQ
        CommandeDto dto = mapToDto(saved);

        // Envoi RabbitMQ
        rabbitTemplate.convertAndSend(exchange, "commande.created", dto,
                message -> message);

        return saved;
    }

    public Commande update(Long id, Commande c) {
        Commande existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande not found"));

        existing.setNumCommande(c.getNumCommande());
        existing.setIdClient(c.getIdClient());
        existing.setAdresseFacturation(c.getAdresseFacturation());
        existing.setAdresseLivraison(c.getAdresseLivraison());
        existing.setStatut(c.getStatut());
        // Remplacer les lignes si nécessaire
        existing.getLignes().clear();
        existing.getLignes().addAll(c.getLignes());
        existing.recalculerMontantTotal();

        Commande updated = repository.save(existing);

        CommandeDto dto = mapToDto(updated);
        rabbitTemplate.convertAndSend(exchange, "commande.updated", dto,
                message -> message);

        return updated;
    }

    public void delete(Long id) {
        Commande existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande not found"));
        repository.deleteById(id);

        CommandeDto dto = mapToDto(existing);
        rabbitTemplate.convertAndSend(exchange, "commande.deleted", dto,
                message -> message);
    }

    public List<Commande> all() {
        return repository.findAll();
    }

    public Commande get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande not found"));
    }

    // --- Méthode privée de mapping d'adresse ---
    private AdresseDto mapAdresse(Adresse adresse) {
        if (adresse == null)
            return null;

        return new AdresseDto(
                adresse.getNumeroRue(),
                adresse.getRue(),
                adresse.getVille(),
                adresse.getCodePostal(),
                adresse.getPays());
    }

    // --- Mapper Commande → CommandeDto ---
    private CommandeDto mapToDto(Commande c) {
        CommandeDto dto = new CommandeDto();
        dto.setNumCommande(c.getNumCommande());
        dto.setDateCreation(c.getDateCreation());
        dto.setIdClient(c.getIdClient());

        dto.setAdresseFacturation(mapAdresse(c.getAdresseFacturation()));
        dto.setAdresseLivraison(mapAdresse(c.getAdresseLivraison()));
        dto.setStatut(c.getStatut());
        // --- Transformation des lignes ---
        List<LigneCommandeDto> lignesDto = c.getLignes().stream()
                .map(ligne -> new LigneCommandeDto(
                        ligne.getProduitId(),
                        ligne.getLibelleProduit(),
                        ligne.getQuantite(),
                        ligne.getPrixUnitaire(),
                        ligne.getMontant()))
                .toList();
        dto.setLignes(lignesDto);
        dto.setMontantTotal(c.getMontantTotal());
        return dto;
    }
}