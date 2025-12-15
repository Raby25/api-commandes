package payetonkawa.api_commande;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.testcontainers.utility.TestcontainersConfiguration;

import payetonkawa.api_commande.Config.TestSecurityConfig;
import payetonkawa.api_commande.config.CommandeProducer;

@SpringBootTest

@Import({ TestSecurityConfig.class })

class ApiCommandeApplicationTests {

	@MockBean
	private CommandeProducer commandeProducer;

	/*
	 * @Test
	 * void contextLoads() {
	 * // Vérifie que le contexte Spring démarre
	 * }
	 */
}
