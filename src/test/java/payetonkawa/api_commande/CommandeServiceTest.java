package payetonkawa.api_commande;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;

import payetonkawa.api_commande.Config.TestSecurityConfig;
import payetonkawa.api_commande.dto.AdresseDto;
import payetonkawa.api_commande.dto.CommandeDto;
import payetonkawa.api_commande.dto.LigneCommandeDto;
import payetonkawa.api_commande.dto.ProductDto;
import payetonkawa.api_commande.model.Commande;
import payetonkawa.api_commande.model.StatutCommande;
import payetonkawa.api_commande.repository.AdresseRepository;
import payetonkawa.api_commande.repository.CommandeRepository;
import payetonkawa.api_commande.repository.LigneCommandeRepository;
import payetonkawa.api_commande.services.CommandeService;
import payetonkawa.api_commande.services.ProduitClient;

@ExtendWith(MockitoExtension.class)
@Import(TestSecurityConfig.class)
class CommandeServiceTest {

    @Mock
    private CommandeRepository commandeRepository;

    @Mock
    private AdresseRepository adresseRepository;

    @Mock
    private LigneCommandeRepository ligneCommandeRepository;

    @Mock
    private ProduitClient produitClient;

    @InjectMocks
    private CommandeService commandeService;

    // ------------------ Happy path ------------------
    @Test
    void create_commande_ok() {
        CommandeDto dto = new CommandeDto();
        dto.setIdClient(1L);
        dto.setStatut(StatutCommande.EN_COURS);
        LigneCommandeDto ligne = new LigneCommandeDto();
        ligne.setProduitId(1L);
        ligne.setQuantite(2);
        dto.setLignes(List.of(ligne));
        AdresseDto adresse = new AdresseDto();
        adresse.setRue("10 Rue de Paris");
        adresse.setVille("Paris");
        adresse.setCodePostal("75001");
        adresse.setPays("France");
        dto.setAdresseLivraison(adresse);
        ProductDto produit = new ProductDto();
        produit.setId(1L);
        produit.setName("Produit Test");
        produit.setPrice(10.0f);
        produit.setStock(10);
        when(produitClient.getProduitById(1L)).thenReturn(produit);
        Commande saved = new Commande();
        saved.setStatut(StatutCommande.EN_COURS);
        when(commandeRepository.save(any(Commande.class))).thenReturn(saved);
        CommandeDto result = commandeService.create(dto);
        assertNotNull(result);
        assertEquals(StatutCommande.EN_COURS, result.getStatut());
        verify(commandeRepository).save(any(Commande.class));
    }

    // ------------------ Exceptions / validations ------------------
    @Test
    void create_dtoNull_nullPointerException() {
        assertThrows(NullPointerException.class, () -> commandeService.create(null));
    }

    @Test
    void create_idClientNull_exception() {
        CommandeDto dto = new CommandeDto();
        dto.setStatut(StatutCommande.EN_COURS);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> commandeService.create(dto));

        assertEquals("idClient est obligatoire", ex.getMessage());
    }

    @Test
    void create_commandeSansLignes_exception() {
        CommandeDto dto = new CommandeDto();
        dto.setIdClient(1L);
        dto.setStatut(StatutCommande.EN_COURS);
        dto.setLignes(List.of()); // vide
        dto.setAdresseLivraison(new AdresseDto());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> commandeService.create(dto));

        assertEquals("La commande doit contenir au moins une ligne", ex.getMessage());
    }

    @Test
    void create_adresseManquante_exception() {
        CommandeDto dto = new CommandeDto();
        dto.setIdClient(1L);
        dto.setStatut(StatutCommande.EN_COURS);

        LigneCommandeDto ligne = new LigneCommandeDto();
        ligne.setProduitId(1L);
        ligne.setQuantite(1);
        dto.setLignes(List.of(ligne));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> commandeService.create(dto));

        assertEquals("L'adresse de livraison est obligatoire", ex.getMessage());
    }

    @Test
    void create_produitInexistant_exception() {
        CommandeDto dto = new CommandeDto();
        dto.setIdClient(1L);
        dto.setStatut(StatutCommande.EN_COURS);

        LigneCommandeDto ligne = new LigneCommandeDto();
        ligne.setProduitId(1L);
        ligne.setQuantite(1);
        dto.setLignes(List.of(ligne));

        AdresseDto adresse = new AdresseDto();
        adresse.setRue("Rue");
        dto.setAdresseLivraison(adresse);

        // Produit inexistant
        when(produitClient.getProduitById(1L)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> commandeService.create(dto));

        // Vérifie que l'exception concerne bien un produit inexistant
        assertTrue(ex.getMessage().toLowerCase().contains("produit"));
    }

    @Test
    void create_stockInsuffisant_exception() {
        CommandeDto dto = new CommandeDto();
        dto.setIdClient(1L);
        dto.setStatut(StatutCommande.EN_COURS);

        LigneCommandeDto ligne = new LigneCommandeDto();
        ligne.setProduitId(1L);
        ligne.setQuantite(5);
        dto.setLignes(List.of(ligne));

        AdresseDto adresse = new AdresseDto();
        adresse.setRue("Rue");
        dto.setAdresseLivraison(adresse);

        ProductDto produit = new ProductDto();
        produit.setId(1L);
        produit.setName("Produit Test");
        produit.setPrice(10.0f);
        produit.setStock(2); // insuffisant
        when(produitClient.getProduitById(1L)).thenReturn(produit);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> commandeService.create(dto));

        assertTrue(ex.getMessage().toLowerCase().contains("stock"));
    }

    @Test
    void findById_existante_ok() {
        Commande commande = new Commande();
        commande.setId(1L);
        when(commandeRepository.findById(1L)).thenReturn(Optional.of(commande));

        CommandeDto result = commandeService.getCommandeById(1L);
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void findById_inexistante_exception() {
        when(commandeRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> commandeService.getCommandeById(1L));

        // assertTrue(ex.getMessage().contains("Commande non trouvée"));
        assertTrue(ex.getMessage().toLowerCase().contains("commande"));
    }

    @Test
    void deleteById_ok() {
        Commande commande = new Commande();
        commande.setId(1L);
        when(commandeRepository.findById(1L)).thenReturn(Optional.of(commande));

        commandeService.delete(1L);

        verify(commandeRepository).deleteById(1L);
    }

    @Test
    void deleteById_inexistante_exception() {
        when(commandeRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> commandeService.delete(1L));

        assertTrue(ex.getMessage().contains("Commande"));
    }

    @Test
    void updateCommande_existante_ok() {
        Commande commande = new Commande();
        commande.setId(1L);
        when(commandeRepository.findById(1L)).thenReturn(Optional.of(commande));
        when(commandeRepository.save(any())).thenReturn(commande);

        CommandeDto dto = new CommandeDto();
        dto.setIdClient(1L);
        dto.setStatut(StatutCommande.LIVREE);
        dto.setLignes(List.of());

        CommandeDto result = commandeService.update(1L, dto);
        assertNotNull(result);
        assertEquals(StatutCommande.LIVREE, result.getStatut());
    }

    @Test
    void updateCommande_inexistante_exception() {
        when(commandeRepository.findById(1L)).thenReturn(Optional.empty());

        CommandeDto dto = new CommandeDto();
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> commandeService.update(1L, dto));

        assertTrue(ex.getMessage().toLowerCase().contains("commande"));
    }

    @Test
    void update_ok() {
        Commande existante = new Commande();
        existante.setId(1L);

        when(commandeRepository.findById(1L))
                .thenReturn(Optional.of(existante));
        when(commandeRepository.save(any()))
                .thenReturn(existante);

        ProductDto produit = new ProductDto();
        produit.setId(1L);
        produit.setStock(10);
        produit.setPrice(5.0f);

        when(produitClient.getProduitById(1L))
                .thenReturn(produit);

        LigneCommandeDto ligne = new LigneCommandeDto();
        ligne.setProduitId(1L);
        ligne.setQuantite(2);

        CommandeDto dto = new CommandeDto();
        dto.setStatut(StatutCommande.EN_COURS);
        dto.setLignes(List.of(ligne));

        CommandeDto result = commandeService.update(1L, dto);

        assertNotNull(result);
        verify(commandeRepository).save(any());
    }

    @Test
    void update_sansLignes_exception() {
        Commande existante = new Commande();
        existante.setId(1L);

        when(commandeRepository.findById(1L))
                .thenReturn(Optional.of(existante));

        CommandeDto dto = new CommandeDto();
        dto.setStatut(StatutCommande.EN_COURS);
        dto.setLignes(null);

        Exception ex = assertThrows(NullPointerException.class,
                () -> commandeService.update(1L, dto));

        assertNotNull(ex);
    }

    /*
     * @Test
     * void delete_ok() {
     * Commande commande = new Commande();
     * commande.setId(1L);
     * 
     * when(commandeRepository.findById(1L))
     * .thenReturn(Optional.of(commande));
     * 
     * doNothing().when(commandeRepository).delete(commande);
     * 
     * assertDoesNotThrow(() -> commandeService.delete(1L));
     * 
     * verify(commandeRepository).delete(commande);
     * }
     */
}
