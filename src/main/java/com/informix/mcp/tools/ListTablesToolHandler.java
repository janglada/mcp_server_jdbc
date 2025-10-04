package com.informix.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.informix.mcp.database.InformixConnection;

import io.modelcontextprotocol.spec.McpSchema;
import  io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP tool handler for listing all tables in the Informix database.
 */
public class ListTablesToolHandler {
    private static final Logger logger = LoggerFactory.getLogger(ListTablesToolHandler.class);
    private final ListTablesTool listTablesTool;
    private final ObjectMapper objectMapper;

    public ListTablesToolHandler(InformixConnection dbConnection, ObjectMapper objectMapper) {
        this.listTablesTool = new ListTablesTool(dbConnection);
        this.objectMapper = objectMapper;
    }

    /**
     * Handle the list_tables tool call.
     */
    public CallToolResult handle(Map<String, Object> arguments) {
        try {
            String schema = arguments != null ? (String) arguments.get("schema") : null;
            List<Map<String, String>> tables = listTablesTool.listTables(schema);

            Map<String, Object> result = new HashMap<>();
            result.put("tables", tables);
            result.put("count", tables.size());

            String jsonResult = objectMapper.writeValueAsString(result);

            logger.info("list_tables returned {} tables", tables.size());

            return new CallToolResult(
                List.of((McpSchema.Content) new McpSchema.TextContent(jsonResult)),
                false
            );
        } catch (Exception e) {
            logger.error("Error in list_tables tool", e);
            return new CallToolResult(
                List.of((McpSchema.Content) new McpSchema.TextContent("Error: " + e.getMessage())),
                true
            );
        }
    }
}
