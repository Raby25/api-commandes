package payetonkawa.api_commande.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

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

    private final String exchange = "commandes.exchange";

    public CommandeDto create(CommandeDto dto) {
        log.info("Tentative de création de commande DTO: {}", dto);

        // VALIDATION DE BASE
        if (dto.getIdClient() == null) {
            throw new RuntimeException("idClient est obligatoire");
        }

        if (dto.getLignes() == null || dto.getLignes().isEmpty()) {
            throw new RuntimeException("La commande doit contenir au moins une ligne");
        }

        // ----- CREATION ENTITE COMMANDE -----
        Commande c = new Commande();
        c.setNumCommande(dto.getNumCommande());
        c.setDateCreation(dto.getDateCreation() != null ? dto.getDateCreation() : LocalDateTime.now());
        c.setIdClient(dto.getIdClient());

        // Vérifier statut
        c.setStatut(dto.getStatut() != null ? dto.getStatut() : StatutCommande.EN_ATTENTE);

        // ----- ADRESSES -----
        Adresse adresseFacturation = findOrCreateAdresse(mapDtoToAdresse(dto.getAdresseFacturation()));
        Adresse adresseLivraison = findOrCreateAdresse(mapDtoToAdresse(dto.getAdresseLivraison()));

        c.setAdresseFacturation(adresseFacturation);
        c.setAdresseLivraison(adresseLivraison);

        // ----- TRAITEMENT LIGNES -----
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
            if (produit == null) {
                throw new RuntimeException("Produit introuvable ID: " + ligneDto.getProduitId());
            }

            // Vérification stock
            if (produit.getStock() < ligneDto.getQuantite()) {
                throw new RuntimeException("Stock insuffisant pour: " + produit.getLibelle());
            }

            // Création ligne commande
            LigneCommande ligne = new LigneCommande();
            ligne.setCommande(c);
            ligne.setProduitId(produit.getId());
            ligne.setLibelleProduit(produit.getLibelle());
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

        // ----- SAUVEGARDE -----
        Commande saved = repository.save(c);
        log.info("Commande enregistrée ID {}", saved.getId());

        // Conversion pour retour
        CommandeDto savedDto = mapToDto(saved);
        log.info("Commande saved DTO: {}", savedDto);

        // ----- Envoi RabbitMQ -----
        rabbitTemplate.convertAndSend(exchange, "commande.created", savedDto);

        return savedDto;
    }

    public CommandeDto update(Long id, Commande c) {
        Commande existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande not found"));

        existing.setNumCommande(c.getNumCommande());
        existing.setIdClient(c.getIdClient());

        existing.setStatut(c.getStatut());
        // --- Gestion des adresses
        if (c.getAdresseFacturation() != null) {
            Adresse updatedFacturation = findOrCreateAdresse(c.getAdresseFacturation());
            existing.setAdresseFacturation(updatedFacturation);
        }

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
                    throw new RuntimeException("Stock insuffisant pour le produit : " + produit.getLibelle());
                }
                // Mise à jour du stock
                produitClient.updateStock(produit.getId(), produit.getStock() - newLine.getQuantite());
            }

            if (newLine.getId() != null && existingLinesMap.containsKey(newLine.getId())) {

                // ========== MISE À JOUR ==========
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

                // ========== AJOUT ==========
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

        // ========== SUPPRESSION des lignes retirées du JSON ==========
        existing.getLignes().clear();
        existing.getLignes().addAll(finalLines);

        existing.recalculerMontantTotal();

        Commande updated = repository.save(existing);

        CommandeDto dto = mapToDto(updated);

        rabbitTemplate.convertAndSend(exchange, "commande.updated", dto,
                message -> message);

        return dto;
    }

    public void delete(Long id) {
        Commande existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande not found"));
        repository.deleteById(id);

        CommandeDto dto = mapToDto(existing);
        rabbitTemplate.convertAndSend(exchange, "commande.deleted", dto,
                message -> message);
    }

    public List<CommandeDto> all() {
        return repository.findAll()
                .stream()
                .map(this::mapToDto) // map chaque Commande en CommandeDto
                .toList();
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
        dto.setId(c.getId());
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

    private Adresse findOrCreateAdresse(Adresse a) {
        if (a == null)
            return null;

        return adresseRepository.findByNumeroRueAndRueAndVilleAndCodePostalAndPays(
                a.getNumeroRue(),
                a.getRue(),
                a.getVille(),
                a.getCodePostal(),
                a.getPays()).orElseGet(() -> adresseRepository.save(a));
    }

    private Adresse mapDtoToAdresse(AdresseDto dto) {
        if (dto == null)
            return null;

        Adresse a = new Adresse();
        a.setNumeroRue(dto.getNumeroRue());
        a.setRue(dto.getRue());
        a.setVille(dto.getVille());
        a.setCodePostal(dto.getCodePostal());
        a.setPays(dto.getPays());
        return a;
    }

}