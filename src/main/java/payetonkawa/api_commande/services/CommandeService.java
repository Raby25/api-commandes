package payetonkawa.api_commande.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        // ===== VALIDATIONS DE BASE =====
        validateCommandeDto(dto);

        // ===== CRÉATION ENTITÉ COMMANDE =====
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

        // ===== ADRESSES =====

        if (dto.getAdresseLivraison() == null) {
            throw new RuntimeException("L'adresse de livraison est obligatoire");
        }

        Adresse adresseLivraison = findOrCreateAdresse(mapDtoToAdresse(dto.getAdresseLivraison()));
        c.setAdresseLivraison(adresseLivraison);

        // ===== TRAITEMENT LIGNES =====
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

            // Création ligne commande
            LigneCommande ligne = new LigneCommande();
            ligne.setCommande(c);
            ligne.setProduitId(produit.getId());
            ligne.setLibelleProduit(produit.getName()); // API utilise "name" pas "libelle"
            ligne.setQuantite(ligneDto.getQuantite());
            ligne.setPrixUnitaire(BigDecimal.valueOf(produit.getPrice()));

            // Calcul du montant
            ligne.calculerMontant();
            total = total.add(ligne.getMontant());

            // Mise à jour du stock
            produitClient.updateStock(produit.getId(), produit.getStock() - ligneDto.getQuantite());

            c.getLignes().add(ligne);
        }

        // Montant total
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
            // On ne fail pas la commande si RabbitMQ échoue
        }

        return savedDto;
    }

    @Transactional
    public CommandeDto update(Long id, Commande c) {
        Commande existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande introuvable avec l'ID: " + id));

        existing.setNumCommande(c.getNumCommande());
        existing.setIdClient(c.getIdClient());
        existing.setStatut(c.getStatut());

        // --- Gestion de l'adresse de livraison

        if (c.getAdresseLivraison() != null) {
            Adresse updatedLivraison = findOrCreateAdresse(c.getAdresseLivraison());
            existing.setAdresseLivraison(updatedLivraison);
        }

        // --- Map des lignes existantes pour éviter les doublons ---
        Map<Long, LigneCommande> existingLinesMap = existing.getLignes().stream()
                .collect(Collectors.toMap(LigneCommande::getId, l -> l));

        List<LigneCommande> finalLines = new ArrayList<>();

        for (LigneCommande newLine : c.getLignes()) {
            // --- Vérification du stock via ProduitClient ---
            if (newLine.getProduitId() != null) {
                ProductDto produit = produitClient.getProduitById(newLine.getProduitId());
                if (produit.getStock() < newLine.getQuantite()) {
                    throw new RuntimeException("Stock insuffisant pour le produit : " + produit.getName());
                }
                // Mise à jour du stock
                produitClient.updateStock(produit.getId(), produit.getStock() - newLine.getQuantite());
            }

            if (newLine.getId() != null && existingLinesMap.containsKey(newLine.getId())) {
                // ===== MISE À JOUR =====
                LigneCommande oldLine = existingLinesMap.get(newLine.getId());
                oldLine.setQuantite(newLine.getQuantite());
                oldLine.setPrixUnitaire(newLine.getPrixUnitaire());
                oldLine.setProduitId(newLine.getProduitId());

                if (newLine.getProduitId() != null) {
                    oldLine.setLibelleProduit(
                            produitClient.getLibelleById(newLine.getProduitId()));
                }

                oldLine.calculerMontant();
                finalLines.add(oldLine);

            } else {
                // ===== AJOUT =====
                newLine.setId(null);
                newLine.setCommande(existing);

                if (newLine.getProduitId() != null) {
                    newLine.setLibelleProduit(
                            produitClient.getLibelleById(newLine.getProduitId()));
                }

                newLine.calculerMontant();
                finalLines.add(newLine);
            }
        }

        // ===== SUPPRESSION des lignes retirées du JSON =====
        existing.getLignes().clear();
        existing.getLignes().addAll(finalLines);

        existing.recalculerMontantTotal();

        Commande updated = repository.save(existing);

        CommandeDto dto = mapToDto(updated);

        try {
            rabbitTemplate.convertAndSend(exchange, "commande.updated", dto);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du message RabbitMQ (update)", e);
        }

        return dto;
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

    public Commande get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande introuvable avec l'ID: " + id));
    }

    // ========== MÉTHODES PRIVÉES ==========

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