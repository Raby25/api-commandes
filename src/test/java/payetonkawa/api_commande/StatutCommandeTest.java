package payetonkawa.api_commande;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import payetonkawa.api_commande.model.StatutCommande;

class StatutCommandeTest {

    @Test
    void statutCommande_values_ok() {
        assertNotNull(StatutCommande.EN_COURS);
        assertNotNull(StatutCommande.LIVREE);
        assertEquals("EN_COURS", StatutCommande.EN_COURS.name());
    }
}
