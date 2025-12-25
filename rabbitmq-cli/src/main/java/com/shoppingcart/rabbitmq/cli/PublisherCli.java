package com.shoppingcart.rabbitmq.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoppingcart.rabbitmq.publisher.Publisher;
import com.shoppingcart.rabbitmq.publisher.PublishOptions;
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Command-line publisher tool for sending messages to RabbitMQ.
 * <p>
 * Usage: sc-mq-publisher [options] <event-type> <json-payload>
 */
@Slf4j
@SpringBootApplication(scanBasePackages = "com.shoppingcart.rabbitmq")
@Command(name = "sc-mq-publisher", mixinStandardHelpOptions = true, version = "1.0.0",
        description = "Publishes messages to RabbitMQ")
@RequiredArgsConstructor
public class PublisherCli implements Callable<Integer>, CommandLineRunner, ExitCodeGenerator {

    private final Publisher publisher;
    private final ObjectMapper objectMapper;

    @Parameters(index = "0", description = "Event type (used as routing key)")
    private String eventType;

    @Parameters(index = "1", description = "JSON payload")
    private String payload;

    @Option(names = {"-e", "--exchange"}, description = "Exchange name", defaultValue = "events")
    private String exchange;

    @Option(names = {"-r", "--routing-key"}, description = "Custom routing key (overrides event-type)")
    private String routingKey;

    @Option(names = {"-H", "--header"}, description = "Custom headers (key=value)")
    private Map<String, String> headers = new HashMap<>();

    @Option(names = {"--no-confirm"}, description = "Don't wait for publisher confirms")
    private boolean noConfirm;

    @Option(names = {"--json-output"}, description = "Output result as JSON")
    private boolean jsonOutput;

    private int exitCode = 0;

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(PublisherCli.class, args)));
    }

    @Override
    public void run(String... args) throws Exception {
        exitCode = new CommandLine(this).execute(args);
    }

    @Override
    public Integer call() {
        try {
            // Use custom routing key or event type
            String rk = routingKey != null ? routingKey : eventType;

            // Build publish options
            PublishOptions options = PublishOptions.builder()
                    .exchange(exchange)
                    .routingKey(rk)
                    .waitForConfirm(!noConfirm)
                    .headers(new HashMap<>(headers))
                    .build();

            // Parse and publish
            Object body;
            try {
                body = objectMapper.readValue(payload, Object.class);
            } catch (Exception e) {
                // Use raw string if not valid JSON
                body = payload;
            }

            publisher.publish(options, body);

            // Output result
            if (jsonOutput) {
                Map<String, Object> result = Map.of(
                        "status", "success",
                        "exchange", exchange,
                        "routingKey", rk,
                        "eventType", eventType
                );
                System.out.println(objectMapper.writeValueAsString(result));
            } else {
                System.out.printf("Published to exchange='%s', routingKey='%s'%n", exchange, rk);
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
