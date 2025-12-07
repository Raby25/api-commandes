package payetonkawa.api_commande.services;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import payetonkawa.api_commande.dto.ProductDto;

@Service
@RequiredArgsConstructor
public class ProduitClient {

    private final RestTemplate restTemplate;

    private final String baseUrl = "http://localhost:8081/produits/";

    // Récupérer tout le produit
    public ProductDto getProduitById(Long produitId) {
        String url = baseUrl + produitId;
        return restTemplate.getForObject(url, ProductDto.class);
    }

    // Récupérer uniquement le libellé
    public String getLibelleById(Long produitId) {
        ProductDto produit = getProduitById(produitId);
        return produit != null ? produit.getLibelle() : null;
    }

    // Mettre à jour le stock
    public void updateStock(Long produitId, int nouveauStock) {
        String url = baseUrl + produitId;
        ProductDto produit = getProduitById(produitId);
        if (produit != null) {
            produit.setStock(nouveauStock);
            restTemplate.put(url, produit);
        } else {
            throw new RuntimeException("Produit introuvable pour mise à jour du stock : " + produitId);
        }
    }
}