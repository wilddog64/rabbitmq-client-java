package com.shoppingcart.rabbitmq.examples;

import com.shoppingcart.rabbitmq.publisher.Publisher;
import com.shoppingcart.rabbitmq.publisher.PublishOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

/**
 * Example application demonstrating message publishing.
 */
@Slf4j
@SpringBootApplication(scanBasePackages = "com.shoppingcart.rabbitmq")
@RequiredArgsConstructor
public class PublisherExample implements CommandLineRunner {

    private final Publisher publisher;

    public static void main(String[] args) {
        SpringApplication.run(PublisherExample.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting publisher example...");

        // Declare exchange
        publisher.declareExchange("events", Publisher.ExchangeType.TOPIC, true, false);

        // Publish simple message
        publisher.publish("events", "order.created", Map.of(
                "orderId", "ORD-12345",
                "customerId", "CUST-001",
                "amount", 99.99
        ));
        log.info("Published order.created event");

        // Publish with custom options
        PublishOptions options = PublishOptions.builder()
                .exchange("events")
                .routingKey("order.updated")
                .priority(5)
                .build()
                .withHeader("source", "example-app");

        publisher.publish(options, Map.of(
                "orderId", "ORD-12345",
                "status", "SHIPPED"
        ));
        log.info("Published order.updated event");

        // Publish multiple messages
        for (int i = 0; i < 10; i++) {
            publisher.publish("events", "order.item.added", Map.of(
                    "orderId", "ORD-12345",
                    "itemId", "ITEM-" + i,
                    "quantity", 1
            ));
        }
        log.info("Published 10 item events");

        log.info("Publisher example completed");
    }
}
