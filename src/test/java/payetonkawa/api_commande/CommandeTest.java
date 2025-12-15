package payetonkawa.api_commande;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import payetonkawa.api_commande.model.Commande;
import payetonkawa.api_commande.model.StatutCommande;

class CommandeTest {

    @Test
    void commande_entity_gettersSetters_ok() {
        Commande commande = new Commande();

        commande.setId(1L);
        commande.setIdClient(2L);
        commande.setStatut(StatutCommande.EN_COURS);

        assertEquals(1L, commande.getId());
        assertEquals(2L, commande.getIdClient());
        assertEquals(StatutCommande.EN_COURS, commande.getStatut());
    }
}