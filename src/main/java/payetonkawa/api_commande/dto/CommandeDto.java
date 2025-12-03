package payetonkawa.api_commande.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import payetonkawa.api_commande.model.StatutCommande;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandeDto {

    private String numCommande;
    private LocalDateTime dateCreation;
    private Long idClient;

    private AdresseDto adresseFacturation;
    private AdresseDto adresseLivraison;

    private StatutCommande statut;
    private List<LigneCommandeDto> lignes;

    private BigDecimal montantTotal;
}
