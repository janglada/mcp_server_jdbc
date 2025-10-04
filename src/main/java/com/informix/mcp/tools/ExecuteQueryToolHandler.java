package com.informix.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.informix.mcp.database.InformixConnection;

import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.List;

/**
 * MCP tool handler for executing SELECT queries.
 */
public class ExecuteQueryToolHandler {
    private static final Logger logger = LoggerFactory.getLogger(ExecuteQueryToolHandler.class);
    private final ExecuteQueryTool executeQueryTool;
    private final ObjectMapper objectMapper;

    public ExecuteQueryToolHandler(InformixConnection dbConnection, ObjectMapper objectMapper) {
        this.executeQueryTool = new ExecuteQueryTool(dbConnection);
        this.objectMapper = objectMapper;
    }

    /**
     * Handle the execute_query tool call.
     */
    public McpSchema.CallToolResult handle(Map<String, Object> arguments) {
        try {
            String query = (String) arguments.get("query");

            if (query == null || query.trim().isEmpty()) {
                return new McpSchema.CallToolResult(
                    List.of( new McpSchema.TextContent("Error: query is required")),
                    true
                );
            }

            int maxRows = arguments.get("max_rows") != null
                ? ((Number) arguments.get("max_rows")).intValue()
                : 10000;

            Map<String, Object> result = executeQueryTool.executeQueryAsMap(query, maxRows);
            String jsonResult = objectMapper.writeValueAsString(result);

            logger.info("execute_query returned {} rows", result.get("rowCount"));

            return new McpSchema.CallToolResult(
                    jsonResult,
                false
            );
        } catch (SecurityException e) {
            logger.warn("Security violation in execute_query: {}", e.getMessage());
            return new McpSchema.CallToolResult(
             "Security Error: " + e.getMessage(),
                true
            );
        } catch (Exception e) {
            logger.error("Error in execute_query tool", e);
            return new McpSchema.CallToolResult(
                "Error: " + e.getMessage(),
                true
            );
        }
    }
}
