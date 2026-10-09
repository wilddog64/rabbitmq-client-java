# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Fixed
- `Integration Tests` CI job no longer fails due to flaky HashiCorp apt key install. Vault CLI
  dependency removed; `Configure Vault` step now configures the Vault service container via its
  HTTP API using `curl`. Vault service image pinned from `latest` to `1.15.6`.

## [1.1.0] - 2026-10-08

### Changed
- **Breaking:** 1.1.x requires Spring Boot 4.0 and Spring Cloud 2025.1. Boot 3 consumers (e.g.
  `shopping-cart-order`) stay on 1.0.x. Motivation: `shopping-cart-payment` must move to Spring
  Boot 4 to clear CVE-2026-47884 (spring-webmvc `XsltView` RCE, fixed only in Spring Framework
  7.0.9), and 1.0.x fails on Boot 4 because the health API moved.
- Upgraded to Spring Boot 4.0.8, Spring Cloud 2025.1.3 (Spring Cloud Vault 5.0.2) and
  Resilience4j 2.4.0 (`resilience4j-spring-boot4`).
- Health indicators now use `org.springframework.boot.health.contributor.{Health,HealthIndicator}`,
  with no behaviour change.
- Message serialization stays on Jackson 2 (`com.fasterxml`), declared explicitly, so the wire
  format consumers see is unchanged.
- Testcontainers 2.0.5 (renamed `testcontainers-*` artifacts); picocli-spring-boot-starter 4.7.7.

## [1.0.1] - 2026-04-11

### Fixed
- `ConnectionManager.getStats()` no longer throws `NullPointerException` when called before any
  AMQP channel has been opened. `CachingConnectionFactory.getCacheProperties()` is now wrapped in
  a try-catch so `/actuator/health` probes succeed during pod startup.
