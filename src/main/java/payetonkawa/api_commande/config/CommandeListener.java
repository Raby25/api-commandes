package payetonkawa.api_commande.config;


import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import payetonkawa.api_commande.model.Commande;

@Component
public class CommandeListener {

    @RabbitListener(queues = "commandes.queue")
    public void receive(Commande commande) {
        System.out.println("Commande reçu depuis RabbitMQ : " + commande);
    }
}