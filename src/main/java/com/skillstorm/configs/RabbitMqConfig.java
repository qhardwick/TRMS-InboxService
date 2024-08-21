package com.skillstorm.configs;

import com.skillstorm.constants.Queues;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Value("${AWS_HOSTNAME:localhost}")
    private String host;

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // Exchanges:
    @Value("${exchanges.direct}")
    private String directExchange;

    // Set up credentials and connect to RabbitMQ:
    @Bean
    public CachingConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(host);
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");
        return connectionFactory;
    }

    // Configure the RabbitTemplate:
    @Bean
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
        rabbitTemplate.setMessageConverter(messageConverter());
        rabbitTemplate.setReplyTimeout(6000);
        return rabbitTemplate;
    }

    // Create the exchange:
    @Bean
    public Exchange directExchange() {
        return new DirectExchange(directExchange);
    }

    // Create the queues:

    // From Form-Service:
    @Bean
    public Queue approvalRequestQueue() {
        return new Queue(Queues.APPROVAL_REQUEST.toString());
    }

    @Bean
    public Queue deletionRequestQueue() { return new Queue(Queues.DELETION_REQUEST.toString()); }


    // To Form-Service:
    @Bean
    public Queue automaticApprovalQueue() {
        return new Queue(Queues.AUTO_APPROVAL.toString());
    }


    // Bind the queues to the exchange:

    // Approval Request binding:
    @Bean
    public Binding approvalRequestBinding(Queue approvalRequestQueue, Exchange directExchange) {
        return BindingBuilder.bind(approvalRequestQueue)
                .to(directExchange)
                .with(Queues.APPROVAL_REQUEST)
                .noargs();
    }

    // Deletion Request binding:
    @Bean
    public Binding deletionRequestBinding(Queue deletionRequestQueue, Exchange directExchange) {
        return BindingBuilder.bind(deletionRequestQueue)
                .to(directExchange)
                .with(Queues.DELETION_REQUEST)
                .noargs();
    }

    // AutomaticApproval binding:
    @Bean
    public Binding automaticApprovalBinding(Queue automaticApprovalQueue, Exchange directExchange) {
        return BindingBuilder.bind(automaticApprovalQueue)
                .to(directExchange)
                .with(Queues.AUTO_APPROVAL)
                .noargs();
    }
}