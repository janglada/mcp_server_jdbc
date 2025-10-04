package com.informix.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.informix.mcp.config.DatabaseConfig;
import com.informix.mcp.database.InformixConnection;
import com.informix.mcp.tools.ExecuteQueryToolHandler;
import com.informix.mcp.tools.GetTableSchemaToolHandler;
import com.informix.mcp.tools.ListTablesToolHandler;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import  io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Jetty server configuration for MCP servlet transport.
 * Configures the embedded Jetty server with MCP servlet endpoint.
 */
public class JettyServerConfig {
    private static final Logger logger = LoggerFactory.getLogger(JettyServerConfig.class);
    private static final String VERSION = "1.0.0";

    private final InformixConnection dbConnection;
    private final ObjectMapper objectMapper;
    private final int port;

    public JettyServerConfig(DatabaseConfig config, int port) {
        this.port = port;
        logger.info("Initializing Jetty MCP Server v{}", VERSION);

        // Initialize database connection
        this.dbConnection = new InformixConnection(config);

        // Test connection on startup
        if (!dbConnection.testConnection()) {
            throw new RuntimeException("Failed to connect to database");
        }

        // Initialize Jackson
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        logger.info("Server initialized successfully");
    }

    /**
     * Create and configure the MCP server.
     */
    public void createMcpServer(HttpServletStreamableServerTransportProvider transport) {
        // Initialize tool handlers
        ListTablesToolHandler listTablesHandler = new ListTablesToolHandler(dbConnection, objectMapper);
        GetTableSchemaToolHandler getTableSchemaHandler = new GetTableSchemaToolHandler(dbConnection, objectMapper);
        ExecuteQueryToolHandler executeQueryHandler = new ExecuteQueryToolHandler(dbConnection, objectMapper);

        // Build the MCP server
        McpServer.sync(transport)
            .serverInfo("informix-mcp-server", VERSION)
            .capabilities(McpSchema.ServerCapabilities.builder()
                .tools(true)
                .build())
            .tool(createListTablesTool(), (exchange, arguments) -> listTablesHandler.handle(arguments))
            .tool(createGetTableSchemaTool(), (exchange, arguments) -> getTableSchemaHandler.handle(arguments))
            .tool(createExecuteQueryTool(), (exchange, arguments) -> executeQueryHandler.handle(arguments))
            .build();

        logger.info("MCP Server initialized with 3 tools: list_tables, get_table_schema, execute_query");
    }

    /**
     * Create and start the Jetty server.
     */
    public Server createJettyServer() {
        Server server = new Server(port);

        // Create servlet context
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // Create MCP transport
        HttpServletStreamableServerTransportProvider transport =
            HttpServletStreamableServerTransportProvider.builder()
                .mcpEndpoint("/mcp/message")
                .build();

        // Create MCP server
        createMcpServer(transport);

        // The transport creates and registers the servlet automatically when the server is built
        // No manual servlet registration needed

        logger.info("MCP endpoint registered at: /mcp/message");
        logger.info("Server will listen on port: {}", port);

        return server;
    }

    /**
     * Create the list_tables tool specification.
     */
    private Tool createListTablesTool() {
        return new Tool(
            "list_tables",
            null, // description
            "List all tables in the Informix database. Optionally filter by schema name.",
            new McpSchema.JsonSchema(null, createListTablesSchema(), null, null, null, null),
            null, // context
            null, // annotations
            null  // extra
        );
    }

    /**
     * Create the get_table_schema tool specification.
     */
    private Tool createGetTableSchemaTool() {
        return new Tool(
            "get_table_schema",
            null, // description
            "Get the schema/structure of a specific table including column names, types, nullable status, and primary keys.",
            new McpSchema.JsonSchema(null, createGetTableSchemaSchema(), null, null, null, null),
            null, // context
            null, // annotations
            null  // extra
        );
    }

    /**
     * Create the execute_query tool specification.
     */
    private Tool createExecuteQueryTool() {
        return new Tool(
            "execute_query",
            null, // description
            "Execute a SELECT query against the database. Only SELECT statements are allowed (read-only). Queries are validated for security.",
            new McpSchema.JsonSchema(null, createExecuteQuerySchema(), null, null, null, null),
            null, // context
            null, // annotations
            null  // extra
        );
    }

    /**
     * Create JSON schema for list_tables tool.
     */
    private Map<String, Object> createListTablesSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> schemaProperty = new HashMap<>();
        schemaProperty.put("type", "string");
        schemaProperty.put("description", "Optional schema name filter");
        properties.put("schema", schemaProperty);

        schema.put("properties", properties);
        return schema;
    }

    /**
     * Create JSON schema for get_table_schema tool.
     */
    private Map<String, Object> createGetTableSchemaSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> tableNameProperty = new HashMap<>();
        tableNameProperty.put("type", "string");
        tableNameProperty.put("description", "Name of the table (can include schema: schema.table)");
        properties.put("table_name", tableNameProperty);

        schema.put("properties", properties);
        schema.put("required", new String[]{"table_name"});
        return schema;
    }

    /**
     * Create JSON schema for execute_query tool.
     */
    private Map<String, Object> createExecuteQuerySchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> queryProperty = new HashMap<>();
        queryProperty.put("type", "string");
        queryProperty.put("description", "SQL SELECT statement to execute (read-only)");
        properties.put("query", queryProperty);

        Map<String, Object> maxRowsProperty = new HashMap<>();
        maxRowsProperty.put("type", "integer");
        maxRowsProperty.put("description", "Maximum number of rows to return");
        maxRowsProperty.put("default", 10000);
        properties.put("max_rows", maxRowsProperty);

        schema.put("properties", properties);
        schema.put("required", new String[]{"query"});
        return schema;
    }

    /**
     * Shutdown the database connection.
     */
    public void shutdown() {
        logger.info("Shutting down MCP Server...");
        if (dbConnection != null) {
            dbConnection.close();
        }
        logger.info("Server shutdown complete");
    }
}
