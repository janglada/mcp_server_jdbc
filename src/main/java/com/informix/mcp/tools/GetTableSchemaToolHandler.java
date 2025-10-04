package com.informix.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.informix.mcp.database.InformixConnection;
import io.modelcontextprotocol.spec.McpSchema;
import  io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.List;

/**
 * MCP tool handler for retrieving table schema information.
 */
public class GetTableSchemaToolHandler {
    private static final Logger logger = LoggerFactory.getLogger(GetTableSchemaToolHandler.class);
    private final GetTableSchemaTool getSchemaTool;
    private final ObjectMapper objectMapper;

    public GetTableSchemaToolHandler(InformixConnection dbConnection, ObjectMapper objectMapper) {
        this.getSchemaTool = new GetTableSchemaTool(dbConnection);
        this.objectMapper = objectMapper;
    }

    /**
     * Handle the get_table_schema tool call.
     */
    public CallToolResult handle(Map<String, Object> arguments) {
        try {
            String tableName = (String) arguments.get("table_name");

            if (tableName == null || tableName.trim().isEmpty()) {
                return new CallToolResult(
                    List.of(new McpSchema.TextContent("Error: table_name is required")),
                    true
                );
            }

            Map<String, Object> schema = getSchemaTool.getTableSchemaAsMap(tableName);
            String jsonResult = objectMapper.writeValueAsString(schema);

            logger.info("get_table_schema returned schema for table: {}", tableName);

            return new CallToolResult(
                List.of(new McpSchema.TextContent(jsonResult)),
                false
            );
        } catch (Exception e) {
            logger.error("Error in get_table_schema tool", e);
            return new CallToolResult(
                List.of(new McpSchema.TextContent("Error: " + e.getMessage())),
                true
            );
        }
    }
}
