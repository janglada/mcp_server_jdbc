package com.informix.mcp.tools;

import com.informix.mcp.database.InformixConnection;
import com.informix.mcp.model.ColumnInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * Tool for retrieving table schema information from Informix database.
 * Returns detailed column metadata including types, constraints, and keys.
 */
public class GetTableSchemaTool {
    private static final Logger logger = LoggerFactory.getLogger(GetTableSchemaTool.class);
    private final InformixConnection dbConnection;

    public GetTableSchemaTool(InformixConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    /**
     * Get complete schema information for a table.
     *
     * @param tableName Name of the table (can include schema: schema.table)
     * @return List of ColumnInfo records
     */
    public List<ColumnInfo> getTableSchema(String tableName) throws SQLException {
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be null or empty");
        }

        logger.info("Getting schema for table: {}", tableName);

        // Parse schema and table name
        String schema = null;
        String table = tableName.trim();

        if (table.contains(".")) {
            String[] parts = table.split("\\.", 2);
            schema = parts[0];
            table = parts[1];
        }

        List<ColumnInfo> columns = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            // Get primary key columns
            Set<String> primaryKeys = getPrimaryKeys(metaData, conn.getCatalog(), schema, table);

            // Get column information
            try (ResultSet rs = metaData.getColumns(conn.getCatalog(), schema, table, "%")) {
                boolean foundColumns = false;

                while (rs.next()) {
                    foundColumns = true;
                    String columnName = rs.getString("COLUMN_NAME");
                    String dataType = rs.getString("TYPE_NAME");
                    int columnSize = rs.getInt("COLUMN_SIZE");
                    int nullable = rs.getInt("NULLABLE");
                    String remarks = rs.getString("REMARKS");

                    boolean isNullable = (nullable == DatabaseMetaData.columnNullable);
                    boolean isPrimaryKey = primaryKeys.contains(columnName);

                    columns.add(new ColumnInfo(
                        columnName,
                        dataType,
                        columnSize,
                        isNullable,
                        isPrimaryKey,
                        remarks
                    ));
                }

                if (!foundColumns) {
                    throw new SQLException("Table not found: " + tableName);
                }
            }

            logger.info("Retrieved schema for table '{}': {} columns", tableName, columns.size());
        } catch (SQLException e) {
            logger.error("Failed to get schema for table: {}", tableName, e);
            throw e;
        }

        return columns;
    }

    /**
     * Get primary key columns for a table.
     */
    private Set<String> getPrimaryKeys(DatabaseMetaData metaData, String catalog,
                                       String schema, String table) throws SQLException {
        Set<String> primaryKeys = new HashSet<>();

        try (ResultSet rs = metaData.getPrimaryKeys(catalog, schema, table)) {
            while (rs.next()) {
                primaryKeys.add(rs.getString("COLUMN_NAME"));
            }
        }

        return primaryKeys;
    }

    /**
     * Get schema as a formatted map for JSON serialization.
     */
    public Map<String, Object> getTableSchemaAsMap(String tableName) throws SQLException {
        List<ColumnInfo> columns = getTableSchema(tableName);

        Map<String, Object> result = new HashMap<>();
        result.put("tableName", tableName);
        result.put("columnCount", columns.size());

        List<Map<String, Object>> columnsList = new ArrayList<>();
        for (ColumnInfo col : columns) {
            Map<String, Object> colMap = new HashMap<>();
            colMap.put("name", col.columnName());
            colMap.put("type", col.dataType());
            colMap.put("size", col.columnSize());
            colMap.put("nullable", col.isNullable());
            colMap.put("primaryKey", col.isPrimaryKey());

            if (col.remarks() != null && !col.remarks().trim().isEmpty()) {
                colMap.put("remarks", col.remarks());
            }

            columnsList.add(colMap);
        }

        result.put("columns", columnsList);

        return result;
    }

    /**
     * Get a human-readable schema description.
     */
    public String getTableSchemaDescription(String tableName) throws SQLException {
        List<ColumnInfo> columns = getTableSchema(tableName);

        StringBuilder sb = new StringBuilder();
        sb.append("Table: ").append(tableName).append("\n");
        sb.append("Columns (").append(columns.size()).append("):\n");
        sb.append("-".repeat(80)).append("\n");

        for (ColumnInfo col : columns) {
            sb.append(String.format("  %-30s %-15s %-10s %s\n",
                col.columnName(),
                col.dataType() + "(" + col.columnSize() + ")",
                col.isNullable() ? "NULL" : "NOT NULL",
                col.isPrimaryKey() ? "PRIMARY KEY" : ""
            ));
        }

        return sb.toString();
    }
}
