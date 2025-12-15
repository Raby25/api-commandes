package payetonkawa.api_commande.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import payetonkawa.api_commande.dto.AdresseDto;
import payetonkawa.api_commande.dto.CommandeDto;
import payetonkawa.api_commande.dto.LigneCommandeDto;
import payetonkawa.api_commande.dto.ProductDto;
import payetonkawa.api_commande.model.*;
import payetonkawa.api_commande.repository.AdresseRepository;
import payetonkawa.api_commande.repository.CommandeRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommandeService {

    private final CommandeRepository repository;
    private final AdresseRepository adresseRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ProduitClient produitClient;

    @Value("${rabbitmq.commandes.exchange}")
    private String exchange;

    @Transactional
    public CommandeDto create(CommandeDto dto) {
        log.info("Tentative de création de commande DTO: {}", dto);

        validateCommandeDto(dto);

        Commande c = new Commande();

        // Générer numéro de commande si absent
        if (dto.getNumCommande() == null || dto.getNumCommande().isEmpty()) {
            c.setNumCommande(generateNumCommande());
        } else {
            c.setNumCommande(dto.getNumCommande());
        }

        c.setDateCreation(dto.getDateCreation() != null ? dto.getDateCreation() : LocalDateTime.now());
        c.setIdClient(dto.getIdClient());
        c.setStatut(dto.getStatut() != null ? dto.getStatut() : StatutCommande.EN_ATTENTE);

        if (dto.getAdresseLivraison() == null) {
            throw new RuntimeException("L'adresse de livraison est obligatoire");
        }

        Adresse adresseLivraison = findOrCreateAdresse(mapDtoToAdresse(dto.getAdresseLivraison()));
        c.setAdresseLivraison(adresseLivraison);

        // lignes
        BigDecimal total = BigDecimal.ZERO;

        for (LigneCommandeDto ligneDto : dto.getLignes()) {

            if (ligneDto.getProduitId() == null) {
                throw new RuntimeException("produitId manquant dans une ligne");
            }

            if (ligneDto.getQuantite() == null || ligneDto.getQuantite() <= 0) {
                throw new RuntimeException("Quantité invalide pour le produit ID: " + ligneDto.getProduitId());
            }

            // Récupération produit dans microservice produit
            ProductDto produit = produitClient.getProduitById(ligneDto.getProduitId());

            // Vérification stock
            if (produit.getStock() < ligneDto.getQuantite()) {
                throw new RuntimeException("Stock insuffisant pour: " + produit.getName() +
                        " (disponible: " + produit.getStock() + ", demandé: " + ligneDto.getQuantite() + ")");
            }

            LigneCommande ligne = new LigneCommande();
            ligne.setCommande(c);
            ligne.setProduitId(produit.getId());
            ligne.setLibelleProduit(produit.getName());
            ligne.setQuantite(ligneDto.getQuantite());
            ligne.setPrixUnitaire(BigDecimal.valueOf(produit.getPrice()));

            ligne.calculerMontant();
            total = total.add(ligne.getMontant());
            produitClient.updateStock(produit.getId(), produit.getStock() - ligneDto.getQuantite());

            c.getLignes().add(ligne);
        }
        c.setMontantTotal(total);

        // ===== SAUVEGARDE =====
        Commande saved = repository.save(c);
        log.info("Commande enregistrée ID {}", saved.getId());

        // Conversion pour retour
        CommandeDto savedDto = mapToDto(saved);
        log.info("Commande saved DTO: {}", savedDto);

        // ===== Envoi RabbitMQ =====
        try {
            rabbitTemplate.convertAndSend(exchange, "commande.created", savedDto);
            log.info("Message RabbitMQ envoyé pour la commande {}", saved.getId());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du message RabbitMQ", e);

        }

        return savedDto;
    }

    @Transactional
    public CommandeDto update(Long id, CommandeDto dto) {
        log.info("Tentative de mise à jour de la commande ID {} avec DTO: {}", id, dto);

        // Récupérer la commande existante
        Commande existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande introuvable avec l'ID: " + id));

        if (dto.getNumCommande() != null && !dto.getNumCommande().isEmpty()) {
            existing.setNumCommande(dto.getNumCommande());
        }
        existing.setIdClient(dto.getIdClient());
        existing.setStatut(dto.getStatut() != null ? dto.getStatut() : existing.getStatut());

        if (dto.getAdresseLivraison() != null) {
            Adresse updatedLivraison = findOrCreateAdresse(mapDtoToAdresse(dto.getAdresseLivraison()));
            existing.setAdresseLivraison(updatedLivraison);
        }

        Map<Long, LigneCommande> existingLinesMap = existing.getLignes().stream()
                .collect(Collectors.toMap(LigneCommande::getId, l -> l));

        List<LigneCommande> finalLines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (LigneCommandeDto ligneDto : dto.getLignes()) {

            if (ligneDto.getProduitId() == null) {
                throw new RuntimeException("produitId manquant dans une ligne");
            }
            if (ligneDto.getQuantite() == null || ligneDto.getQuantite() <= 0) {
                throw new RuntimeException("Quantité invalide pour le produit ID: " + ligneDto.getProduitId());
            }

            // Récupération produit et vérification stock
            ProductDto produit = produitClient.getProduitById(ligneDto.getProduitId());
            if (produit.getStock() < ligneDto.getQuantite()) {
                throw new RuntimeException("Stock insuffisant pour: " + produit.getName());
            }

            LigneCommande ligne;

            if (ligneDto.getId() != null && existingLinesMap.containsKey(ligneDto.getId())) {
                ligne = existingLinesMap.get(ligneDto.getId());
                ligne.setQuantite(ligneDto.getQuantite());
                ligne.setProduitId(ligneDto.getProduitId());

                if (ligneDto.getPrixUnitaire() != null) {
                    ligne.setPrixUnitaire(ligneDto.getPrixUnitaire());
                } else {
                    ligne.setPrixUnitaire(BigDecimal.valueOf(produit.getPrice()));
                }

                ligne.setLibelleProduit(produit.getName());

            } else {
                // NOUVELLE LIGNE
                ligne = new LigneCommande();
                ligne.setCommande(existing);
                ligne.setProduitId(produit.getId());
                ligne.setLibelleProduit(produit.getName());
                ligne.setQuantite(ligneDto.getQuantite());
                ligne.setPrixUnitaire(BigDecimal.valueOf(produit.getPrice()));
            }

            // Lier ligne à la commande et calcul montant
            ligne.setCommande(existing);
            ligne.calculerMontant();
            total = total.add(ligne.getMontant());

            // Mettre à jour le stock
            produitClient.updateStock(produit.getId(), produit.getStock() - ligneDto.getQuantite());

            finalLines.add(ligne);
        }

        existing.getLignes().clear();
        existing.getLignes().addAll(finalLines);

        existing.setMontantTotal(total);

        Commande saved = repository.save(existing);
        CommandeDto savedDto = mapToDto(saved);
        log.info("Commande mise à jour DTO: {}", savedDto);

        //ENVOI RABBITMQ
        try {
            rabbitTemplate.convertAndSend(exchange, "commande.updated", savedDto);
            log.info("Message RabbitMQ envoyé pour la commande {}", saved.getId());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du message RabbitMQ (update)", e);
        }

        return savedDto;
    }

    @Transactional
    public void delete(Long id) {
        Commande existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande introuvable avec l'ID: " + id));

        repository.deleteById(id);

        CommandeDto dto = mapToDto(existing);

        try {
            rabbitTemplate.convertAndSend(exchange, "commande.deleted", dto);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du message RabbitMQ (delete)", e);
        }
    }

    public List<CommandeDto> all() {
        return repository.findAll()
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public CommandeDto getCommandeById(Long id) {
        Commande c = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande introuvable avec l'ID: " + id));

        return mapToDto(c);
    }

    @Transactional
    public CommandeDto updateCommandeLignes(Long id, List<LigneCommandeDto> lignesDto) {
        Commande existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande introuvable avec l'ID: " + id));

        Map<Long, LigneCommande> existingLinesMap = existing.getLignes().stream()
                .collect(Collectors.toMap(LigneCommande::getId, l -> l));

        List<LigneCommande> finalLines = new ArrayList<>();

        for (LigneCommandeDto ligneDto : lignesDto) {
            if (ligneDto.getId() != null && existingLinesMap.containsKey(ligneDto.getId())) {
                LigneCommande oldLine = existingLinesMap.get(ligneDto.getId());
                oldLine.setQuantite(ligneDto.getQuantite());
                oldLine.setPrixUnitaire(ligneDto.getPrixUnitaire());
                oldLine.setProduitId(ligneDto.getProduitId());
                oldLine.setLibelleProduit(ligneDto.getLibelleProduit());
                oldLine.calculerMontant();
                finalLines.add(oldLine);
            } else {
                LigneCommande newLine = new LigneCommande();
                newLine.setCommande(existing);
                newLine.setProduitId(ligneDto.getProduitId());
                newLine.setLibelleProduit(ligneDto.getLibelleProduit());
                newLine.setQuantite(ligneDto.getQuantite());
                newLine.setPrixUnitaire(ligneDto.getPrixUnitaire());
                newLine.calculerMontant();
                finalLines.add(newLine);
            }
        }

        existing.getLignes().clear();
        existing.getLignes().addAll(finalLines);
        existing.recalculerMontantTotal();

        Commande updated = repository.save(existing);
        CommandeDto dto = mapToDto(updated);

        // --- Envoi message RabbitMQ ---
        try {
            rabbitTemplate.convertAndSend(exchange, "commande.updated", dto);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du message RabbitMQ (update lignes)", e);
        }

        return dto;
    }

    private void validateCommandeDto(CommandeDto dto) {
        if (dto.getIdClient() == null) {
            throw new RuntimeException("idClient est obligatoire");
        }

        if (dto.getLignes() == null || dto.getLignes().isEmpty()) {
            throw new RuntimeException("La commande doit contenir au moins une ligne");
        }
    }

    private String generateNumCommande() {
        return "CMD-" + System.currentTimeMillis();
    }

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

    private CommandeDto mapToDto(Commande c) {
        CommandeDto dto = new CommandeDto();
        dto.setId(c.getId());
        dto.setNumCommande(c.getNumCommande());
        dto.setDateCreation(c.getDateCreation());
        dto.setIdClient(c.getIdClient());
        dto.setAdresseLivraison(mapAdresse(c.getAdresseLivraison()));
        dto.setStatut(c.getStatut());

        List<LigneCommandeDto> lignesDto = c.getLignes().stream()
                .map(ligne -> new LigneCommandeDto(ligne.getId(),
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

    public List<CommandeDto> getCommandesByClientId(Long clientId) {
        List<Commande> commandes = repository.findByIdClient(clientId);
        return commandes.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<LigneCommandeDto> getProductsByClientIdAndCommandeId(Long clientId, Long commandeId) {
        Commande commande = repository.findById(commandeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande introuvable"));

        if (!commande.getIdClient().equals(clientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette commande n'appartient pas au client");
        }

        return commande.getLignes().stream()
                .map(ligne -> new LigneCommandeDto(
                        ligne.getId(),
                        ligne.getProduitId(),
                        ligne.getLibelleProduit(),
                        ligne.getQuantite(),
                        ligne.getPrixUnitaire(),
                        ligne.getMontant()))
                .collect(Collectors.toList());
    }

    private Adresse findOrCreateAdresse(Adresse a) {
        if (a == null) {
            throw new RuntimeException("Adresse ne peut pas être null");
        }

        return adresseRepository.findByNumeroRueAndRueAndVilleAndCodePostalAndPays(
                a.getNumeroRue(),
                a.getRue(),
                a.getVille(),
                a.getCodePostal(),
                a.getPays())
                .orElseGet(() -> adresseRepository.save(a));
    }

    private Adresse mapDtoToAdresse(AdresseDto dto) {
        if (dto == null) {
            return null;
        }

        Adresse a = new Adresse();
        a.setNumeroRue(dto.getNumeroRue());
        a.setRue(dto.getRue());
        a.setVille(dto.getVille());
        a.setCodePostal(dto.getCodePostal());
        a.setPays(dto.getPays());
        return a;
    }
}