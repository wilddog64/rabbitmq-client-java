package com.shoppingcart.rabbitmq.connection;

import com.shoppingcart.rabbitmq.config.RabbitMQProperties;
import com.shoppingcart.rabbitmq.exception.ConnectionException;
import com.shoppingcart.rabbitmq.vault.VaultCredentialManager;
import com.shoppingcart.rabbitmq.vault.VaultCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages RabbitMQ connections with Vault credential integration.
 * <p>
 * This component handles:
 * - Creating and configuring connection factories
 * - Integrating with Vault for dynamic credentials
 * - Connection event handling
 * - Graceful shutdown
 */
@Slf4j
public class ConnectionManager {

    private final RabbitMQProperties properties;
    private final VaultCredentialManager vaultCredentialManager;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private CachingConnectionFactory connectionFactory;

    public ConnectionManager(RabbitMQProperties properties, VaultCredentialManager vaultCredentialManager) {
        this.properties = properties;
        this.vaultCredentialManager = vaultCredentialManager;
    }

    /**
     * Gets or creates the connection factory.
     *
     * @return The configured connection factory
     */
    public ConnectionFactory getConnectionFactory() {
        if (!initialized.get()) {
            synchronized (this) {
                if (!initialized.get()) {
                    initialize();
                }
            }
        }
        return connectionFactory;
    }

    /**
     * Initializes the connection factory with Vault credentials.
     */
    private void initialize() {
        log.info("Initializing RabbitMQ connection factory, host={}:{}, vhost={}",
                properties.getHost(), properties.getPort(), properties.getVhost());

        try {
            // Get credentials from Vault
            VaultCredentials credentials = vaultCredentialManager.getCredentials();

            // Create connection factory
            connectionFactory = new CachingConnectionFactory();
            connectionFactory.setHost(properties.getHost());
            connectionFactory.setPort(properties.getPort());
            connectionFactory.setVirtualHost(properties.getVhost());
            connectionFactory.setUsername(credentials.username());
            connectionFactory.setPassword(credentials.password());

            // Configure connection settings
            connectionFactory.setRequestedHeartBeat(
                    (int) properties.getPool().getHeartbeat().toSeconds());
            connectionFactory.setConnectionTimeout(
                    (int) properties.getPool().getAcquireTimeout().toMillis());

            // Configure channel caching
            connectionFactory.setCacheMode(CachingConnectionFactory.CacheMode.CHANNEL);
            connectionFactory.setChannelCacheSize(properties.getPool().getSize());

            // Enable publisher confirms
            connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
            connectionFactory.setPublisherReturns(true);

            // Add connection listener
            connectionFactory.addConnectionListener(connection -> {
                log.info("RabbitMQ connection established: {}", connection);
            });

            initialized.set(true);
            log.info("Successfully initialized RabbitMQ connection factory");

        } catch (Exception e) {
            log.error("Failed to initialize RabbitMQ connection factory", e);
            throw new ConnectionException(
                    properties.getHost(),
                    properties.getPort(),
                    properties.getVhost(),
                    "Failed to initialize connection factory",
                    e
            );
        }
    }

    /**
     * Refreshes the connection with new credentials from Vault.
     */
    public void refreshCredentials() {
        log.info("Refreshing RabbitMQ connection credentials");

        try {
            VaultCredentials credentials = vaultCredentialManager.fetchCredentials();

            if (connectionFactory != null) {
                connectionFactory.setUsername(credentials.username());
                connectionFactory.setPassword(credentials.password());
                connectionFactory.resetConnection();
                log.info("Successfully refreshed connection credentials");
            }
        } catch (Exception e) {
            log.error("Failed to refresh connection credentials", e);
            throw new ConnectionException(
                    properties.getHost(),
                    properties.getPort(),
                    properties.getVhost(),
                    "Failed to refresh credentials",
                    e
            );
        }
    }

    /**
     * Checks if the connection is healthy.
     */
    public boolean isHealthy() {
        if (connectionFactory == null) {
            return false;
        }

        try {
            var connection = connectionFactory.createConnection();
            boolean open = connection.isOpen();
            return open;
        } catch (Exception e) {
            log.warn("Health check failed", e);
            return false;
        }
    }

    /**
     * Closes the connection manager and releases resources.
     */
    @PreDestroy
    public void close() {
        log.info("Closing RabbitMQ connection manager");

        if (connectionFactory != null) {
            try {
                connectionFactory.destroy();
                log.info("Successfully closed connection factory");
            } catch (Exception e) {
                log.warn("Error closing connection factory", e);
            }
        }

        // Revoke Vault credentials
        if (vaultCredentialManager != null) {
            vaultCredentialManager.revokeCredentials();
        }

        initialized.set(false);
    }

    /**
     * Gets connection statistics.
     */
    public ConnectionStats getStats() {
        if (connectionFactory == null) {
            return new ConnectionStats(0, 0, 0, false);
        }

        var cacheProperties = connectionFactory.getCacheProperties();
        int channelCacheSize = Integer.parseInt(
                cacheProperties.getOrDefault("channelCacheSize", "0").toString());
        int idleChannels = Integer.parseInt(
                cacheProperties.getOrDefault("idleChannelsNotTx", "0").toString());

        return new ConnectionStats(
                channelCacheSize,
                channelCacheSize - idleChannels,
                idleChannels,
                isHealthy()
        );
    }

    /**
     * Connection statistics record.
     */
    public record ConnectionStats(
            int totalChannels,
            int activeChannels,
            int idleChannels,
            boolean healthy
    ) {}
}
