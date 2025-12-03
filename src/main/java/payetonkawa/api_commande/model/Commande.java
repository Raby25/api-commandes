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

    // --------- ADRESSES ----------
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "adresse_facturation_id", nullable = false)
    private Adresse adresseFacturation;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "adresse_livraison_id", nullable = false)
    private Adresse adresseLivraison;

    // --------- STATUT ----------
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutCommande statut = StatutCommande.EN_ATTENTE;

    // --------- LIGNES ----------
    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneCommande> lignes = new ArrayList<>();

    @Column(nullable = false)
    private BigDecimal montantTotal = BigDecimal.ZERO;

    // --- Ajouter une ligne ---
    public void ajouterLigne(LigneCommande ligne) {
        ligne.setCommande(this);
        lignes.add(ligne);
        recalculerMontantTotal();
    }

    // --- Supprimer une ligne ---
    public void supprimerLigne(LigneCommande ligne) {
        lignes.remove(ligne);
        ligne.setCommande(null);
        recalculerMontantTotal();
    }

    // --- Recalcul du montant total ---
    public void recalculerMontantTotal() {
        this.montantTotal = lignes.stream()
                .map(LigneCommande::getMontant)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
