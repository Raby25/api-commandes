package payetonkawa.api_commande.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LigneCommande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long produitId;

    private String libelleProduit;

    @Setter(AccessLevel.NONE)
    private Integer quantite;

    @Setter(AccessLevel.NONE)
    private BigDecimal prixUnitaire;

    private BigDecimal montant;

    @ManyToOne
    @JoinColumn(name = "commande_id")
    private Commande commande;

    public void setQuantite(Integer quantite) {
        this.quantite = quantite;
        calculerMontant();
    }

    public void setPrixUnitaire(BigDecimal prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
        calculerMontant();
    }

    public void calculerMontant() {
        if (prixUnitaire != null && quantite != null) {
            montant = prixUnitaire.multiply(BigDecimal.valueOf(quantite));
        }
    }
}
