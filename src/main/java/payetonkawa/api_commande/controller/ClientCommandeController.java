package payetonkawa.api_commande.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import payetonkawa.api_commande.dto.CommandeDto;
import payetonkawa.api_commande.dto.LigneCommandeDto;
import payetonkawa.api_commande.services.CommandeService;

@RestController
@RequestMapping("/clients")
public class ClientCommandeController {

    private final CommandeService commandeService;

    public ClientCommandeController(CommandeService commandeService) {
        this.commandeService = commandeService;
    }

    // Récupérer toutes les commandes d’un client
    @GetMapping("/{clientId}/commandes")
    public ResponseEntity<List<CommandeDto>> getCommandesByClient(@PathVariable Long clientId) {
        List<CommandeDto> commandes = commandeService.getCommandesByClientId(clientId);
        return ResponseEntity.ok(commandes);
    }

    // Récupérer les produits d’une commande spécifique d’un client
    @GetMapping("/{clientId}/commandes/{commandeId}/products")
    public ResponseEntity<List<LigneCommandeDto>> getProductsFromCommande(
            @PathVariable Long clientId,
            @PathVariable Long commandeId) {
        try {
            List<LigneCommandeDto> products = commandeService.getProductsByClientIdAndCommandeId(clientId, commandeId);
            return ResponseEntity.ok(products);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    
}
