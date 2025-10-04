package com.informix.mcp.database;

import com.informix.mcp.config.DatabaseConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Manages database connection pool for Informix using HikariCP.
 * Provides thread-safe connection management with automatic pooling.
 */
public class InformixConnection {
    private static final Logger logger = LoggerFactory.getLogger(InformixConnection.class);
    private final HikariDataSource dataSource;
    private final DatabaseConfig config;

    public InformixConnection(DatabaseConfig config) {
        this.config = config;
        this.dataSource = createDataSource();
        logger.info("Initialized Informix connection pool: {}", config);
    }

    private HikariDataSource createDataSource() {
        HikariConfig hikariConfig = new HikariConfig();

        // Connection settings
        hikariConfig.setJdbcUrl(config.getJdbcUrl());
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.setDriverClassName("com.informix.jdbc.IfxDriver");

        // Pool settings
        hikariConfig.setMaximumPoolSize(config.getPoolSize());
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setConnectionTimeout(30000); // 30 seconds
        hikariConfig.setIdleTimeout(600000); // 10 minutes
        hikariConfig.setMaxLifetime(1800000); // 30 minutes

        // Connection test query
        hikariConfig.setConnectionTestQuery("SELECT 1 FROM systables WHERE tabid=1");

        // Pool name
        hikariConfig.setPoolName("InformixMCPPool");

        // Performance settings
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        logger.info("Creating HikariCP data source for: {}", config.getJdbcUrl());

        try {
            return new HikariDataSource(hikariConfig);
        } catch (Exception e) {
            logger.error("Failed to create data source", e);
            throw new RuntimeException("Failed to initialize database connection pool", e);
        }
    }

    /**
     * Get a connection from the pool.
     * Caller is responsible for closing the connection.
     */
    public Connection getConnection() throws SQLException {
        try {
            Connection conn = dataSource.getConnection();
            logger.debug("Connection acquired from pool");
            return conn;
        } catch (SQLException e) {
            logger.error("Failed to get connection from pool", e);
            throw e;
        }
    }

    /**
     * Test if the database connection is working.
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn.isValid(5); // 5 second timeout
        } catch (SQLException e) {
            logger.error("Connection test failed", e);
            return false;
        }
    }

    /**
     * Get pool statistics for monitoring.
     */
    public String getPoolStats() {
        if (dataSource == null) {
            return "DataSource not initialized";
        }
        return String.format("Pool Stats - Active: %d, Idle: %d, Total: %d, Waiting: %d",
            dataSource.getHikariPoolMXBean().getActiveConnections(),
            dataSource.getHikariPoolMXBean().getIdleConnections(),
            dataSource.getHikariPoolMXBean().getTotalConnections(),
            dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
        );
    }

    /**
     * Close the connection pool and release all resources.
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Closing connection pool");
            dataSource.close();
        }
    }

    public DatabaseConfig getConfig() {
        return config;
    }
}
