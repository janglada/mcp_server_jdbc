package com.informix.mcp;

import com.informix.mcp.config.DatabaseConfig;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;

import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpTransport;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the MCP Server using the MCP client.
 * Tests end-to-end connectivity and tool invocations.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MCPServerIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(MCPServerIntegrationTest.class);
    private static final int TEST_PORT = 9090;
    private static final String MCP_ENDPOINT = "http://localhost:" + TEST_PORT + "/mcp/message";

    private Server jettyServer;
    private JettyServerConfig serverConfig;
    private McpSyncClient mcpClient;

    @BeforeAll
    void setUp() throws Exception {
/*
        logger.info("Starting MCP Server for integration tests on port {}", TEST_PORT);

        // Create test database configuration
        // Note: You may need to set up environment variables or use a test database
        DatabaseConfig config = createTestDatabaseConfig();

        // Create and start the server
        serverConfig = new JettyServerConfig(config, TEST_PORT);
        jettyServer = serverConfig.createJettyServer();
        jettyServer.start();

        // Wait a moment for server to be fully ready
        Thread.sleep(1000);
*/
        logger.info("MCP Server started successfully at {}", MCP_ENDPOINT);

        // Create MCP client with HTTP transport
        McpClientTransport transport =  io.modelcontextprotocol.client.transport.HttpClientSseClientTransport
                .builder(MCP_ENDPOINT)
                .build();


        mcpClient = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(10))
                .capabilities(McpSchema.ClientCapabilities.builder()
                .roots(true)      // Enable roots capability
                .sampling()       // Enable sampling capability
                .elicitation()    // Enable elicitation capability
                .build())
    .build();



        logger.info("MCP Client initialized successfully");
    }

    @AfterAll
    void tearDown() throws Exception {
        logger.info("Shutting down MCP Server and client");

        if (mcpClient != null) {
            mcpClient.close();
        }

        if (jettyServer != null) {
            jettyServer.stop();
        }

        if (serverConfig != null) {
            serverConfig.shutdown();
        }

        logger.info("Shutdown complete");
    }

//    @Test
//    @DisplayName("Test MCP client can connect and initialize with server")
//    void testClientConnection() throws Exception {
//        // Send initialize request
//        McpSchema.InitializeRequest initRequest = new McpSchema.InitializeRequest(
//            McpSchema.LATEST_PROTOCOL_VERSION,
//            new McpSchema.ClientCapabilities(
//                null, // experimental
//                null, // roots
//                null, // sampling
//                null  // extra
//            ),
//            new McpSchema.Implementation("test-client", "1.0.0")
//        );
//
//        McpSchema.InitializeResult result = mcpClient.initialize(initRequest);
//
//        assertNotNull(result, "Initialize result should not be null");
//        assertNotNull(result.serverInfo(), "Server info should not be null");
//        assertEquals("informix-mcp-server", result.serverInfo().name());
//        assertEquals("1.0.0", result.serverInfo().version());
//
//        logger.info("Server info: {} v{}", result.serverInfo().name(), result.serverInfo().version());
//    }

    @Test
    @DisplayName("Test listing available tools")
    void testListTools() throws Exception {
        // Initialize client first
        initializeClient();

        // List available tools
        McpSchema.ListToolsResult toolsResult = mcpClient.listTools(
        );

        assertNotNull(toolsResult, "Tools result should not be null");
        assertNotNull(toolsResult.tools(), "Tools list should not be null");
        assertEquals(3, toolsResult.tools().size(), "Should have 3 tools");

        // Verify tool names
        List<String> toolNames = toolsResult.tools().stream()
            .map(McpSchema.Tool::name)
            .toList();

        assertTrue(toolNames.contains("list_tables"), "Should have list_tables tool");
        assertTrue(toolNames.contains("get_table_schema"), "Should have get_table_schema tool");
        assertTrue(toolNames.contains("execute_query"), "Should have execute_query tool");

        logger.info("Available tools: {}", toolNames);
    }

    @Test
    @DisplayName("Test calling list_tables tool")
    void testCallListTablesTool() throws Exception {
        // Initialize client first
        initializeClient();

        // Call list_tables tool
        Map<String, Object> arguments = new HashMap<>();
        // Optional: add schema filter
        // arguments.put("schema", "informix");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
            "list_tables",
            arguments,
            null // metadata
        );

        McpSchema.CallToolResult result = mcpClient.callTool(request);

        assertNotNull(result, "Tool result should not be null");
        assertNotNull(result.content(), "Tool content should not be null");
        assertFalse(result.content().isEmpty(), "Tool content should not be empty");
        assertFalse(result.isError(), "Tool should not return error");

        logger.info("list_tables result: {}", result.content());
    }

    @Test
    @DisplayName("Test calling get_table_schema tool")
    void testCallGetTableSchemaTool() throws Exception {
        // Initialize client first
        initializeClient();

        // Call get_table_schema tool
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("table_name", "systables"); // Using a system table that should exist

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
            "get_table_schema",
            arguments,
            null // metadata
        );

        McpSchema.CallToolResult result = mcpClient.callTool(request);

        assertNotNull(result, "Tool result should not be null");
        assertNotNull(result.content(), "Tool content should not be null");
        assertFalse(result.content().isEmpty(), "Tool content should not be empty");
        assertFalse(result.isError(), "Tool should not return error");

        logger.info("get_table_schema result: {}", result.content());
    }

    @Test
    @DisplayName("Test calling execute_query tool with valid SELECT")
    void testCallExecuteQueryTool() throws Exception {
        // Initialize client first
        initializeClient();

        // Call execute_query tool
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("query", "SELECT * FROM systables WHERE tabid = 1");
        arguments.put("max_rows", 10);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
            "execute_query",
            arguments,
            null // metadata
        );

        McpSchema.CallToolResult result = mcpClient.callTool(request);

        assertNotNull(result, "Tool result should not be null");
        assertNotNull(result.content(), "Tool content should not be null");
        assertFalse(result.content().isEmpty(), "Tool content should not be empty");
        assertFalse(result.isError(), "Tool should not return error");

        logger.info("execute_query result: {}", result.content());
    }

//    @Test
//    @DisplayName("Test execute_query tool rejects non-SELECT queries")
//    void testExecuteQueryRejectsInvalidQuery() throws Exception {
//        // Initialize client first
//        initializeClient();
//
//        // Try to call execute_query with an UPDATE statement
//        Map<String, Object> arguments = new HashMap<>();
//        arguments.put("query", "UPDATE systables SET tabname = 'hacked'");
//
//        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
//            "execute_query",
//            arguments,
//            null // metadata
//        );
//
//        McpSchema.CallToolResult result = mcpClient.callTool(request);
//
//        assertNotNull(result, "Tool result should not be null");
//        assertTrue(result.isError(), "Tool should return error for invalid query");
//
//        logger.info("Invalid query correctly rejected: {}", result.content());
//    }

    /**
     * Helper method to initialize the client.
     */
    private void initializeClient() throws Exception {
//        McpSchema.InitializeRequest initRequest = new McpSchema.InitializeRequest(
//            McpSchema.LATEST_PROTOCOL_VERSION,
//            new McpSchema.ClientCapabilities(
//                null, // experimental
//                null, // roots
//                null, // sampling
//                null  // extra
//            ),
//            new McpSchema.Implementation("test-client", "1.0.0")
//        );
//
//        mcpClient.initialize(initRequest);
    }

    /**
     * Create a test database configuration.
     * Override this method or set environment variables for your test database.
     */
    private DatabaseConfig createTestDatabaseConfig() {
        // Try to load from environment first
        try {
            return DatabaseConfig.fromEnvironment();
        } catch (Exception e) {
            logger.warn("Could not load config from environment, using defaults: {}", e.getMessage());

            // Fallback to default test configuration
            // Note: Update these values for your test environment
            return new DatabaseConfig(
                "localhost",
                9088,
                "testdb",
                "informix",
                "in4mix",
                "informix",
                5
            );
        }
    }
}
