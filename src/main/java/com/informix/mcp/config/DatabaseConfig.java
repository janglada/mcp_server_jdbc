package com.informix.mcp.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration class for Informix database connection settings.
 * Supports loading from properties file or environment variables.
 */
public class DatabaseConfig {
    private final String host;
    private final int port;
    private final String database;
    private final String serverName;
    private final String username;
    private final String password;
    private final int poolSize;

    public DatabaseConfig(String host, String database, String serverName,
                         String username, String password) {
        this(host, 9088, database, serverName, username, password, 5);
    }

    /**
     * Create config for Informix Docker container with defaults.
     */
    public static DatabaseConfig forDocker() {
        return new DatabaseConfig(
            "localhost",
            9088,
            "sysmaster",
            "informix",
            "informix",
            "in4mix",
            5
        );
    }

    public DatabaseConfig(String host, int port, String database, String serverName,
                         String username, String password, int poolSize) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.serverName = serverName;
        this.username = username;
        this.password = password;
        this.poolSize = poolSize;
    }

    /**
     * Load configuration from properties file.
     * Falls back to environment variables if file not found.
     */
    public static DatabaseConfig fromPropertiesFile(String filePath) throws IOException {
        Properties props = new Properties();

        try (InputStream input = new FileInputStream(filePath)) {
            props.load(input);
        } catch (IOException e) {
            // Fall back to environment variables
            return fromEnvironment();
        }

        return new DatabaseConfig(
            props.getProperty("informix.host", "localhost"),
            Integer.parseInt(props.getProperty("informix.port", "9088")),
            props.getProperty("informix.database", "sysmaster"),
            props.getProperty("informix.server", "informix"),
            props.getProperty("informix.username", "informix"),
            props.getProperty("informix.password", "in4mix"),
            Integer.parseInt(props.getProperty("informix.pool.size", "5"))
        );
    }

    /**
     * Load configuration from environment variables.
     * Defaults match Informix Docker container.
     */
    public static DatabaseConfig fromEnvironment() {
        return new DatabaseConfig(
            getEnv("INFORMIX_HOST", "localhost"),
            Integer.parseInt(getEnv("INFORMIX_PORT", "9088")),
            getEnv("INFORMIX_DATABASE", "sysmaster"),
            getEnv("INFORMIX_SERVER", "informix"),
            getEnv("INFORMIX_USERNAME", "informix"),
            getEnv("INFORMIX_PASSWORD", "in4mix"),
            Integer.parseInt(getEnv("INFORMIX_POOL_SIZE", "5"))
        );
    }

    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Build JDBC connection URL for Informix.
     */
    public String getJdbcUrl() {
        return String.format("jdbc:informix-sqli://%s:%d/%s:INFORMIXSERVER=%s",
            host, port, database, serverName);
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getDatabase() { return database; }
    public String getServerName() { return serverName; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public int getPoolSize() { return poolSize; }

    @Override
    public String toString() {
        return String.format("DatabaseConfig{host='%s', port=%d, database='%s', server='%s', username='%s', poolSize=%d}",
            host, port, database, serverName, username, poolSize);
    }
}
