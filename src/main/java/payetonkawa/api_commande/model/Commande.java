package payetonkawa.api_commande.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "commandes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Commande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String numCommande;

    @Column(nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(nullable = false)
    private Long idClient;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "adresse_livraison_id", nullable = false)
    private Adresse adresseLivraison;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutCommande statut = StatutCommande.EN_ATTENTE;

    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneCommande> lignes = new ArrayList<>();

    @Column(nullable = false)
    private BigDecimal montantTotal = BigDecimal.ZERO;

    public void ajouterLigne(LigneCommande ligne) {
        ligne.setCommande(this);
        ligne.calculerMontant();
        lignes.add(ligne);
        recalculerMontantTotal();
    }

    public void supprimerLigne(LigneCommande ligne) {
        lignes.remove(ligne);
        ligne.setCommande(null);
        recalculerMontantTotal();
    }

    public void recalculerMontantTotal() {
        this.montantTotal = lignes.stream()
                .peek(LigneCommande::calculerMontant)
                .map(LigneCommande::getMontant)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
