package payetonkawa.api_commande.config;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import payetonkawa.api_commande.dto.CommandeDto;

@Component
public class CommandeProducer implements CommandLineRunner {

    private final RabbitTemplate rabbitTemplate;
    private final AmqpAdmin amqpAdmin;

    @Value("${rabbitmq.commandes.exchange}")
    private String exchange;

    @Value("${rabbitmq.commandes.routing-key}")
    private String routingKey;

    @Value("${rabbitmq.commandes.queue}")
    private String queueName;

    public CommandeProducer(RabbitTemplate rabbitTemplate, AmqpAdmin amqpAdmin) {
        this.rabbitTemplate = rabbitTemplate;
        this.amqpAdmin = amqpAdmin;
    }

    @Override
    public void run(String... args) throws Exception {
        // Vérifie que la queue existe avant d’envoyer
        while (amqpAdmin.getQueueProperties(queueName) == null) {
            System.out.println("Waiting for RabbitMQ queue to be ready...");
            Thread.sleep(1000);
        }

        // Exemple de message de test
        CommandeDto testCommande = new CommandeDto();
        testCommande.setNumCommande("CMD-1001");
        testCommande.setIdClient(1L);
        testCommande.setMontantTotal(null); // ou BigDecimal.valueOf(123.45)
        testCommande.setStatut(null); // ou StatutCommande.EN_ATTENTE

        rabbitTemplate.convertAndSend(exchange, routingKey, testCommande);
        System.out.println("CommandeMessageDTO envoyé !");
    }
}
