# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Fixed
- `ConnectionManager.getStats()` no longer throws `NullPointerException` when called before any
  AMQP channel has been opened. `CachingConnectionFactory.getCacheProperties()` is now wrapped in
  a try-catch so `/actuator/health` probes succeed during pod startup.
