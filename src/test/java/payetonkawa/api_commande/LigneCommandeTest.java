package payetonkawa.api_commande;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import payetonkawa.api_commande.model.LigneCommande;

class LigneCommandeTest {

    @Test
    void ligneCommande_entity_gettersSetters_ok() {
        LigneCommande ligne = new LigneCommande();

        ligne.setId(1L);
        ligne.setProduitId(3L);
        ligne.setQuantite(2);

        ligne.setPrixUnitaire(new BigDecimal("10.0"));

        assertEquals(3L, ligne.getProduitId());
        assertEquals(2, ligne.getQuantite());
        assertEquals(new BigDecimal("10.0"), ligne.getPrixUnitaire());
    }
}