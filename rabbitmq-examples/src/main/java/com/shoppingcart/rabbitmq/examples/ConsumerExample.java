package com.shoppingcart.rabbitmq.examples;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoppingcart.rabbitmq.consumer.Consumer;
import com.shoppingcart.rabbitmq.consumer.ConsumeOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Example application demonstrating message consumption.
 */
@Slf4j
@SpringBootApplication(scanBasePackages = "com.shoppingcart.rabbitmq")
@RequiredArgsConstructor
public class ConsumerExample implements CommandLineRunner {

    private final Consumer consumer;
    private final ObjectMapper objectMapper;

    public static void main(String[] args) {
        SpringApplication.run(ConsumerExample.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting consumer example...");

        // Declare and bind queue
        consumer.declareQueue("order-events", ConsumeOptions.QueueOptions.defaults());
        consumer.bindQueue("order-events", "events", "order.#");

        CountDownLatch latch = new CountDownLatch(10);

        // Start consuming with manual ack
        ConsumeOptions options = ConsumeOptions.builder()
                .queue("order-events")
                .autoAck(false)
                .prefetchCount(5)
                .build();

        String consumerId = consumer.consume(options, message -> {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            Map<String, Object> data = objectMapper.readValue(body, Map.class);

            log.info("Received message: routingKey={}, data={}",
                    message.getMessageProperties().getReceivedRoutingKey(),
                    data);

            // Simulate processing
            Thread.sleep(100);

            latch.countDown();
        });

        log.info("Consumer started with ID: {}", consumerId);

        // Wait for messages or timeout
        boolean received = latch.await(30, TimeUnit.SECONDS);
        if (received) {
            log.info("Received all expected messages");
        } else {
            log.warn("Timeout waiting for messages");
        }

        // Stop consumer
        consumer.stop(consumerId);
        log.info("Consumer example completed");
    }
}
