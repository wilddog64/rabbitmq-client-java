package com.shoppingcart.rabbitmq.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoppingcart.rabbitmq.consumer.Consumer;
import com.shoppingcart.rabbitmq.consumer.ConsumeOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Command-line consumer tool for receiving messages from RabbitMQ.
 * <p>
 * Usage: sc-mq-consumer [options] <queue-name>
 */
@Slf4j
@SpringBootApplication(scanBasePackages = "com.shoppingcart.rabbitmq")
@Command(name = "sc-mq-consumer", mixinStandardHelpOptions = true, version = "1.0.0",
        description = "Consumes messages from RabbitMQ")
@RequiredArgsConstructor
public class ConsumerCli implements Callable<Integer>, CommandLineRunner, ExitCodeGenerator {

    private final Consumer consumer;
    private final ObjectMapper objectMapper;

    @Parameters(index = "0", description = "Queue name to consume from")
    private String queueName;

    @Option(names = {"-n", "--count"}, description = "Number of messages to consume (0 = unlimited)", defaultValue = "0")
    private int count;

    @Option(names = {"--auto-ack"}, description = "Use auto-acknowledgment")
    private boolean autoAck;

    @Option(names = {"--prefetch"}, description = "Prefetch count", defaultValue = "10")
    private int prefetch;

    @Option(names = {"--json-output"}, description = "Output messages as JSON")
    private boolean jsonOutput;

    @Option(names = {"--quiet"}, description = "Only output message bodies")
    private boolean quiet;

    private int exitCode = 0;
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);
    private final AtomicInteger messagesReceived = new AtomicInteger(0);

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(ConsumerCli.class, args)));
    }

    @Override
    public void run(String... args) throws Exception {
        exitCode = new CommandLine(this).execute(args);
    }

    @Override
    public Integer call() {
        try {
            // Register shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Shutting down consumer...");
                consumer.stopAll();
                shutdownLatch.countDown();
            }));

            // Build consume options
            ConsumeOptions options = ConsumeOptions.builder()
                    .queue(queueName)
                    .autoAck(autoAck)
                    .prefetchCount(prefetch)
                    .build();

            if (!quiet) {
                System.out.printf("Consuming from queue '%s'... (Ctrl+C to stop)%n", queueName);
            }

            // Start consuming
            consumer.consume(options, message -> {
                int received = messagesReceived.incrementAndGet();
                String body = new String(message.getBody(), StandardCharsets.UTF_8);

                if (jsonOutput) {
                    Map<String, Object> output = Map.of(
                            "messageId", message.getMessageProperties().getMessageId(),
                            "exchange", message.getMessageProperties().getReceivedExchange(),
                            "routingKey", message.getMessageProperties().getReceivedRoutingKey(),
                            "timestamp", message.getMessageProperties().getTimestamp(),
                            "body", body
                    );
                    System.out.println(objectMapper.writeValueAsString(output));
                } else if (quiet) {
                    System.out.println(body);
                } else {
                    System.out.printf("[%d] Exchange: %s, RoutingKey: %s%n",
                            received,
                            message.getMessageProperties().getReceivedExchange(),
                            message.getMessageProperties().getReceivedRoutingKey());
                    System.out.println(body);
                    System.out.println("---");
                }

                // Check if we've received enough messages
                if (count > 0 && received >= count) {
                    log.info("Received {} messages, stopping", received);
                    shutdownLatch.countDown();
                }
            });

            // Wait for shutdown
            shutdownLatch.await();

            if (!quiet) {
                System.out.printf("Received %d messages%n", messagesReceived.get());
            }

            return 0;

        } catch (Exception e) {
            if (jsonOutput) {
                try {
                    Map<String, Object> result = Map.of(
                            "status", "error",
                            "error", e.getMessage()
                    );
                    System.err.println(objectMapper.writeValueAsString(result));
                } catch (Exception ignored) {
                    System.err.println("{\"status\":\"error\",\"error\":\"" + e.getMessage() + "\"}");
                }
            } else {
                System.err.println("Error: " + e.getMessage());
            }
            return 1;
        }
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
