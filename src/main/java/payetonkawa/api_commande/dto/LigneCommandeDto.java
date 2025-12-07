package payetonkawa.api_commande.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LigneCommandeDto {

    private Long produitId;
    private String libelleProduit;
    private Integer quantite;
    private BigDecimal prixUnitaire;
    private BigDecimal montant;

}
