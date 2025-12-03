package payetonkawa.api_commande.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

@Configuration
public class RabbitConfig {

    @Value("${rabbitmq.commandes.queue}")
    private String queueName;

    @Value("${rabbitmq.commandes.exchange}")
    private String exchangeName;

    @Value("${rabbitmq.commandes.routing-key}")
    private String routingKey;

    @Bean
    public Queue commandesQueue() {
        return new Queue(queueName, false);
    }

    @Bean
    public TopicExchange commandesExchange() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    public Binding binding(Queue commandesQueue, TopicExchange commandesExchange) {
        return BindingBuilder.bind(commandesQueue).to(commandesExchange).with(routingKey);
    }
    // Déclare un MessageConverter JSON pour envoyer/recevoir des objets

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
