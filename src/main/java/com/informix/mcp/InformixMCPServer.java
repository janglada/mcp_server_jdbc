package com.informix.mcp;

import com.informix.mcp.config.DatabaseConfig;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Main entry point for the Informix MCP Server with embedded Jetty.
 * This server implements the Model Context Protocol for read-only database operations.
 *
 * The server exposes three tools via HTTP at /mcp/message:
 * 1. list_tables - List all tables in the database
 * 2. get_table_schema - Get schema information for a specific table
 * 3. execute_query - Execute a SELECT query (read-only)
 */
public class InformixMCPServer {
    private static final Logger logger = LoggerFactory.getLogger(InformixMCPServer.class);
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;

        // Allow port override via command line argument
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                logger.warn("Invalid port number '{}', using default port {}", args[0], DEFAULT_PORT);
            }
        }

        // Allow port override via environment variable
        String portEnv = System.getenv("MCP_PORT");
        if (portEnv != null) {
            try {
                port = Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                logger.warn("Invalid MCP_PORT '{}', using default port {}", portEnv, DEFAULT_PORT);
            }
        }

        try {
            // Load database configuration
            String configFile = System.getenv("INFORMIX_CONFIG_FILE");
            DatabaseConfig config;

            if (configFile != null) {
                logger.info("Loading configuration from file: {}", configFile);
                config = DatabaseConfig.fromPropertiesFile(configFile);
            } else {
                logger.info("Loading configuration from environment variables");
                config = DatabaseConfig.fromEnvironment();
            }

            // Create server configuration
            JettyServerConfig serverConfig = new JettyServerConfig(config, port);

            // Create and start Jetty server
            Server jettyServer = serverConfig.createJettyServer();

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    logger.info("Shutdown signal received");
                    jettyServer.stop();
                    serverConfig.shutdown();
                } catch (Exception e) {
                    logger.error("Error during shutdown", e);
                }
            }));

            // Start the server
            logger.info("Starting Informix MCP Server on port {}...", port);
            jettyServer.start();
            logger.info("Server started successfully!");
            logger.info("MCP endpoint available at: http://localhost:{}/mcp/message", port);
            logger.info("Press Ctrl+C to stop the server");

            // Wait for the server to be stopped
            jettyServer.join();

        } catch (IOException e) {
            logger.error("Failed to load configuration", e);
            System.exit(1);
        } catch (Exception e) {
            logger.error("Fatal error starting server", e);
            System.exit(1);
        }
    }
}
