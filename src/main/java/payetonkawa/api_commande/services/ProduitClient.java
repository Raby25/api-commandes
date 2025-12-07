package payetonkawa.api_commande.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import payetonkawa.api_commande.dto.ProductDto;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProduitClient {

    private final RestTemplate restTemplate;

    @Value("${api.produits.url:http://localhost:8081/products/}")
    private String baseUrl;

    @PostConstruct
    public void init() {
        log.info("========================================");
        log.info("ProduitClient initialisé avec baseUrl: {}", baseUrl);
        log.info("========================================");
    }

    // Récupérer tout le produit
    public ProductDto getProduitById(Long produitId) {
        try {
            String url = baseUrl + produitId;
            log.info("Appel API Produits: {}", url);
            ProductDto produit = restTemplate.getForObject(url, ProductDto.class);

            if (produit == null) {
                throw new RuntimeException("Produit introuvable avec l'ID: " + produitId);
            }

            return produit;
        } catch (RestClientException e) {
            log.error("Erreur lors de la récupération du produit {}: {}", produitId, e.getMessage());
            throw new RuntimeException("Service Produits indisponible pour l'ID: " + produitId, e);
        }
    }

    // Récupérer uniquement le libellé (qui s'appelle "name" dans l'API)
    public String getLibelleById(Long produitId) {
        ProductDto produit = getProduitById(produitId);
        return produit != null ? produit.getName() : null;
    }

    // Mettre à jour le stock
    public void updateStock(Long produitId, int nouveauStock) {
        try {
            String url = baseUrl + produitId;
            ProductDto produit = getProduitById(produitId);

            if (produit == null) {
                throw new RuntimeException("Produit introuvable pour mise à jour du stock: " + produitId);
            }

            produit.setStock(nouveauStock);
            restTemplate.put(url, produit);
            log.info("Stock mis à jour pour le produit {}: nouveau stock = {}", produitId, nouveauStock);

        } catch (RestClientException e) {
            log.error("Erreur lors de la mise à jour du stock du produit {}: {}", produitId, e.getMessage());
            throw new RuntimeException("Impossible de mettre à jour le stock du produit: " + produitId, e);
        }
    }
}