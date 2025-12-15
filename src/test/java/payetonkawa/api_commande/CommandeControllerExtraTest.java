package payetonkawa.api_commande;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import payetonkawa.api_commande.controller.CommandeController;
import payetonkawa.api_commande.services.CommandeService;

@WebMvcTest(CommandeController.class)
class CommandeControllerExtraTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommandeService commandeService;

    /*
     * @Test
     * void getCommandeById_exception_generique_404() throws Exception {
     * when(commandeService.getCommandeById(1L))
     * .thenThrow(new RuntimeException("Erreur"));
     * 
     * mockMvc.perform(get("/commandes/1"))
     * .andExpect(status().isNotFound());
     * }
     */
}
