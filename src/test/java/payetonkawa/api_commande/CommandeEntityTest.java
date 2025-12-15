package payetonkawa.api_commande;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import payetonkawa.api_commande.model.Commande;
import payetonkawa.api_commande.model.LigneCommande;
import payetonkawa.api_commande.model.StatutCommande;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CommandeEntityTest {

    private Commande commande;
    private LigneCommande ligne1;
    private LigneCommande ligne2;

    @BeforeEach
    void setUp() {
        ligne1 = new LigneCommande();
        ligne1.setId(1L);
        ligne1.setId(100L);
        ligne1.setLibelleProduit("Café");
        ligne1.setQuantite(2);
        ligne1.setPrixUnitaire(new BigDecimal("5.50"));

        ligne2 = new LigneCommande();
        ligne2.setId(2L);
        ligne2.setId(101L);
        ligne2.setLibelleProduit("Thé");
        ligne2.setQuantite(1);
        ligne2.setPrixUnitaire(new BigDecimal("3.00"));

        commande = new Commande();
        commande.setId(1L);
        commande.setNumCommande("CMD-001");
        commande.setIdClient(50L);
        commande.setStatut(StatutCommande.EN_ATTENTE);
    }

    @Test
    void commande_gettersSetters_ok() {
        // Act & Assert
        assertEquals(1L, commande.getId());
        assertEquals("CMD-001", commande.getNumCommande());
        assertEquals(50L, commande.getIdClient());
        assertEquals(StatutCommande.EN_ATTENTE, commande.getStatut());
    }

    @Test
    void commande_ajouterLignes_ok() {
        // Arrange
        commande.setLignes(new ArrayList<>());

        // Act
        commande.getLignes().add(ligne1);
        commande.getLignes().add(ligne2);

        // Assert
        assertEquals(2, commande.getLignes().size());
        assertEquals("Café", commande.getLignes().get(0).getLibelleProduit());
        assertEquals("Thé", commande.getLignes().get(1).getLibelleProduit());
    }

    @Test
    void commande_calculerMontantTotal_ok() {
        // Arrange
        commande.setLignes(Arrays.asList(ligne1, ligne2));

        // Act
        BigDecimal total = commande.getLignes().stream()
                .map(l -> l.getPrixUnitaire().multiply(new BigDecimal(l.getQuantite())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Assert
        assertEquals(new BigDecimal("14.00"), total); // 2*5.50 + 1*3.00 = 14.00
    }

    @Test
    void ligneCommande_gettersSetters_ok() {
        // Assert
        assertNotNull(ligne1.getId());
        assertEquals(100L, ligne1.getId());
        assertEquals("Café", ligne1.getLibelleProduit());
        assertEquals(2, ligne1.getQuantite());
        assertEquals(new BigDecimal("5.50"), ligne1.getPrixUnitaire());
    }

    @Test
    void ligneCommande_calculerMontant_ok() {
        // Act
        BigDecimal montant = ligne1.getPrixUnitaire()
                .multiply(new BigDecimal(ligne1.getQuantite()));

        // Assert
        assertEquals(new BigDecimal("11.00"), montant); // 2 * 5.50
    }

    @Test
    void ligneCommande_associerCommande_ok() {
        // Act
        ligne1.setCommande(commande);

        // Assert
        assertNotNull(ligne1.getCommande());
        assertEquals(commande.getId(), ligne1.getCommande().getId());
    }

    @Test
    void statutCommande_valeurs_ok() {
        // Assert
        assertNotNull(StatutCommande.EN_ATTENTE);
        assertNotNull(StatutCommande.EN_COURS);
        assertNotNull(StatutCommande.EXPEDIEE);
        assertNotNull(StatutCommande.LIVREE);
        assertNotNull(StatutCommande.ANNULEE);
    }

    @Test
    void statutCommande_valueOf_ok() {
        // Act
        StatutCommande statut = StatutCommande.valueOf("EN_COURS");

        // Assert
        assertEquals(StatutCommande.EN_COURS, statut);
    }

    @Test
    void commande_changerStatut_ok() {
        // Arrange
        assertEquals(StatutCommande.EN_ATTENTE, commande.getStatut());

        // Act
        commande.setStatut(StatutCommande.EN_COURS);

        // Assert
        assertEquals(StatutCommande.EN_COURS, commande.getStatut());
    }

    @Test
    void commande_toString_contientInformations() {
        // Act
        String result = commande.toString();

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("CMD-001") || result.contains("Commande"));
    }

    @Test
    void ligneCommande_avecPrixNull_neCrashPas() {
        // Arrange
        LigneCommande ligneNull = new LigneCommande();
        ligneNull.setQuantite(2);
        ligneNull.setPrixUnitaire(null);

        // Act & Assert - ne devrait pas crasher
        assertNull(ligneNull.getPrixUnitaire());
    }

    @Test
    void commande_avecLignesVides_ok() {
        // Arrange
        commande.setLignes(new ArrayList<>());

        // Assert
        assertTrue(commande.getLignes().isEmpty());
    }
}
