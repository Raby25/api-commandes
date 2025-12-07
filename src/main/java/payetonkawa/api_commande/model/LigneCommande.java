package payetonkawa.api_commande.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
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
        try {
            if (prixUnitaire != null && quantite != null && quantite > 0) {
                montant = prixUnitaire.multiply(BigDecimal.valueOf(quantite));
            } else {
                log.warn("Impossible de calculer le montant: prixUnitaire={}, quantite={}", prixUnitaire, quantite);
                montant = BigDecimal.ZERO;
            }
        } catch (Exception e) {
            log.error("Erreur lors du calcul du montant", e);
            montant = BigDecimal.ZERO;
        }

    }
}
