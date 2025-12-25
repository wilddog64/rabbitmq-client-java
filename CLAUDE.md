# CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

## Project Overview

**RabbitMQ Client Library (Java)** is a production-ready client library for RabbitMQ with integrated HashiCorp Vault support for dynamic credential management. Built with Spring Boot 3.2 and Java 21.

## Technology Stack

- **Java**: 21 (LTS with virtual threads)
- **Build Tool**: Maven
- **Framework**: Spring Boot 3.2, Spring Cloud 2023
- **RabbitMQ Client**: Spring AMQP
- **Vault Integration**: Spring Cloud Vault
- **Circuit Breaker**: Resilience4j
- **Metrics**: Micrometer + Prometheus
- **Logging**: SLF4J + Logback
- **Testing**: JUnit 5, Mockito, Testcontainers

## Repository Structure

```
rabbitmq-client-java/
├── pom.xml                         # Parent POM
├── CLAUDE.md                       # This file
├── README.md                       # Documentation
├── Jenkinsfile                     # Jenkins CI/CD
├── .github/workflows/java-ci.yml   # GitHub Actions
├── rabbitmq-client/                # Core library module
│   ├── pom.xml
│   └── src/main/java/com/shoppingcart/rabbitmq/
│       ├── config/                 # Configuration classes
│       ├── connection/             # Connection management
│       ├── vault/                  # Vault integration
│       ├── publisher/              # Message publishing
│       ├── consumer/               # Message consumption
│       ├── retry/                  # Retry logic
│       ├── circuitbreaker/         # Circuit breaker
│       ├── metrics/                # Prometheus metrics
│       ├── health/                 # Health indicators
│       ├── dlq/                    # Dead letter queue
│       └── exception/              # Custom exceptions
├── rabbitmq-cli/                   # CLI tools module
│   ├── pom.xml
│   └── src/main/java/com/shoppingcart/rabbitmq/cli/
├── rabbitmq-examples/              # Examples module
│   ├── pom.xml
│   └── src/main/java/com/shoppingcart/rabbitmq/examples/
└── docs/                           # Documentation
```

## Build Commands

```bash
# Build all modules
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Run unit tests
mvn test

# Run integration tests
mvn verify -P integration-tests

# Generate coverage report
mvn jacoco:report

# Package CLI tools
mvn package -pl rabbitmq-cli -am
```

## Key Design Principles

1. **Vault Integration First**: All credentials from HashiCorp Vault
2. **Automatic Reconnection**: Exponential backoff with jitter
3. **Circuit Breaker**: Resilience4j for fault tolerance
4. **Observability**: Micrometer metrics, Spring Actuator health
5. **Spring Boot Native**: Auto-configuration, @ConfigurationProperties

## Configuration

Configuration via `application.yml` or environment variables:

```yaml
rabbitmq:
  host: localhost
  port: 5672
  vhost: /
  use-tls: false

  pool:
    size: 10
    prefetch-count: 10

  retry:
    max-attempts: 3
    initial-interval: 1s
    max-interval: 30s
    multiplier: 2.0

  circuit-breaker:
    enabled: true
    failure-threshold: 5
    success-threshold: 2
    timeout: 30s

spring:
  cloud:
    vault:
      uri: http://localhost:8200
      token: ${VAULT_TOKEN}
      rabbitmq:
        role: rabbitmq-role
        backend: rabbitmq
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `VAULT_ADDR` | Vault server address | `http://localhost:8200` |
| `VAULT_TOKEN` | Vault authentication token | - |
| `RABBITMQ_HOST` | RabbitMQ hostname | `localhost` |
| `RABBITMQ_PORT` | RabbitMQ port | `5672` |

## Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests (requires Docker)
```bash
mvn verify -P integration-tests
```

### Coverage Report
```bash
mvn jacoco:report
open rabbitmq-client/target/site/jacoco/index.html
```

## Code Style

- Follow Google Java Style Guide
- Use `@Slf4j` for logging
- Prefer records for DTOs
- Use `Optional` instead of null
- Use `var` for local variables when type is obvious

## Commit Message Format

```
type(scope): subject

body

footer
```

Types: `feat`, `fix`, `docs`, `test`, `refactor`, `perf`, `chore`

## Related Repositories

- **rabbitmq-client-go**: Go implementation (complete)
- **rabbitmq-client-library**: Python implementation (complete)
- **shopping-cart-infra**: RabbitMQ infrastructure (K8s, Vault)
