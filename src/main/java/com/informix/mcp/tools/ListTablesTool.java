package com.informix.mcp.tools;

import com.informix.mcp.database.InformixConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool for listing all tables in the Informix database.
 * Returns table names with optional schema filtering.
 */
public class ListTablesTool {
    private static final Logger logger = LoggerFactory.getLogger(ListTablesTool.class);
    private final InformixConnection dbConnection;

    public ListTablesTool(InformixConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    /**
     * List all tables in the database.
     *
     * @param schemaPattern Optional schema filter (null for all schemas)
     * @return List of table information maps
     */
    public List<Map<String, String>> listTables(String schemaPattern) throws SQLException {
        logger.info("Listing tables with schema pattern: {}", schemaPattern);
        List<Map<String, String>> tables = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            // Get tables (TABLE type only, not views or system tables)
            try (ResultSet rs = metaData.getTables(
                    conn.getCatalog(),
                    schemaPattern,
                    "%",
                    new String[]{"TABLE"}
            )) {
                while (rs.next()) {
                    Map<String, String> tableInfo = new HashMap<>();
                    tableInfo.put("name", rs.getString("TABLE_NAME"));
                    tableInfo.put("schema", rs.getString("TABLE_SCHEM"));
                    tableInfo.put("type", rs.getString("TABLE_TYPE"));

                    String remarks = rs.getString("REMARKS");
                    if (remarks != null && !remarks.trim().isEmpty()) {
                        tableInfo.put("remarks", remarks);
                    }

                    tables.add(tableInfo);
                }
            }

            logger.info("Found {} tables", tables.size());
        } catch (SQLException e) {
            logger.error("Failed to list tables", e);
            throw e;
        }

        return tables;
    }

    /**
     * List all tables (no schema filter).
     */
    public List<Map<String, String>> listTables() throws SQLException {
        return listTables(null);
    }

    /**
     * Get just the table names as a simple list.
     */
    public List<String> getTableNames(String schemaPattern) throws SQLException {
        List<Map<String, String>> tables = listTables(schemaPattern);
        return tables.stream()
                .map(t -> {
                    String schema = t.get("schema");
                    String name = t.get("name");
                    return (schema != null && !schema.isEmpty()) ? schema + "." + name : name;
                })
                .toList();
    }

    /**
     * Get just the table names as a simple list (no schema filter).
     */
    public List<String> getTableNames() throws SQLException {
        return getTableNames(null);
    }
}
