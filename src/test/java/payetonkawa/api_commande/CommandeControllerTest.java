package payetonkawa.api_commande;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import payetonkawa.api_commande.Config.TestSecurityConfig;
import payetonkawa.api_commande.controller.CommandeController;
import payetonkawa.api_commande.dto.CommandeDto;
import payetonkawa.api_commande.services.CommandeService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@WebMvcTest(CommandeController.class)
@Import(TestSecurityConfig.class)
class CommandeControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private CommandeService commandeService;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        void getCommandeById_existante_ok() throws Exception {
                CommandeDto dto = new CommandeDto();
                dto.setIdClient(1L);
                dto.setId(1L);
                when(commandeService.getCommandeById(1L)).thenReturn(dto);

                mockMvc.perform(get("/commandes/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1L));
        }

        @Test
        void getCommandeById_inexistante_notFound() throws Exception {
                when(commandeService.getCommandeById(1L))
                                .thenThrow(new RuntimeException("Commande introuvable"));
                mockMvc.perform(get("/commandes/1"))
                                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "test", roles = { "COMMANDE_READ" })
        void createCommande_ok() throws Exception {
                CommandeDto dto = new CommandeDto();
                dto.setIdClient(1L);

                when(commandeService.create(any())).thenReturn(dto);

                mockMvc.perform(post("/commandes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isCreated()) // 201
                                .andExpect(jsonPath("$.idClient").value(1L));
        }

        

}
