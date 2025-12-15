package payetonkawa.api_commande;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import payetonkawa.api_commande.dto.CommandeDto;
import payetonkawa.api_commande.dto.LigneCommandeDto;

class CommandeDtoTest {

    @Test
    void commandeDto_gettersSetters_ok() {
        CommandeDto dto = new CommandeDto();

        dto.setId(1L);
        dto.setIdClient(10L);
        dto.setStatut(null);

        LigneCommandeDto ligne = new LigneCommandeDto();
        ligne.setProduitId(5L);
        ligne.setQuantite(2);

        dto.setLignes(List.of(ligne));

        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getIdClient());
        assertEquals(1, dto.getLignes().size());
        assertEquals(5L, dto.getLignes().get(0).getProduitId());
    }
}