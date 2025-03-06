package com.skillstorm.configs;

import com.skillstorm.constants.Queues;
import com.skillstorm.services.MessageService;
import com.skillstorm.services.MessageServiceImpl;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
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

    /* Manual Ack will be a work in progress for now
    // Configure the MessageListenerAdapter to inject into the container and tell it which methods to monitor:
    @Bean
    public  MessageListenerAdapter messageListenerAdapter(MessageServiceImpl messageService, MessageConverter messageConverter) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(messageService);
        adapter.setMessageConverter(messageConverter);
        adapter.setDefaultListenerMethod("postApprovalRequestToInboxByUsername");
        return adapter;
    }

    // Configure the listener container and set AcknowledgeMode to manual to prevent premature acknowledgements
    // when consuming Spring Webflux classes:
    @Bean
    public MessageListenerContainer messageListenerContainer(ConnectionFactory connectionFactory, MessageListenerAdapter messageListenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setMessageListener(messageListenerAdapter);
        container.setQueues(approvalRequestQueue(), deletionRequestQueue(), completionVerificationQueue());
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL);

        return container;
    }
    */

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

    @Bean Queue completionVerificationQueue() { return new Queue(Queues.COMPLETION_VERIFICATION.toString()); }


    // To Form-Service:
    @Bean
    public Queue automaticApprovalQueue() {
        return new Queue(Queues.AUTO_APPROVAL.toString());
    }

    // Internal queues for implementing SSEs:
    @Bean
    public Queue approvalRequestUpdatesQueue() {
        return new Queue(Queues.APPROVAL_REQUEST_UPDATES.toString(), true);
    }


    // Bind the queues to the exchange:

    // Approval Request bindings:
    @Bean
    public Binding approvalRequestBinding(Queue approvalRequestQueue, Exchange directExchange) {
        return BindingBuilder.bind(approvalRequestQueue)
                .to(directExchange)
                .with(Queues.APPROVAL_REQUEST)
                .noargs();
    }

    @Bean
    public Binding approvalRequestUpdatesBinding(Queue approvalRequestUpdatesQueue, Exchange directExchange) {
        return BindingBuilder.bind(approvalRequestUpdatesQueue)
                .to(directExchange)
                .with("user.*")
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

    // Completion Verification binding:
    @Bean
    public Binding completionVerificationBinding(Queue completionVerificationQueue, Exchange directExchange) {
        return BindingBuilder.bind(completionVerificationQueue)
                .to(directExchange)
                .with(Queues.COMPLETION_VERIFICATION)
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