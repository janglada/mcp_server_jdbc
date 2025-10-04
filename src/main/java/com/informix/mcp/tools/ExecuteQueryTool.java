package com.informix.mcp.tools;

import com.informix.mcp.database.InformixConnection;
import com.informix.mcp.database.QueryValidator;
import com.informix.mcp.model.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * Tool for executing SELECT queries against the Informix database.
 * Only allows read-only SELECT statements with strict validation.
 */
public class ExecuteQueryTool {
    private static final Logger logger = LoggerFactory.getLogger(ExecuteQueryTool.class);
    private final InformixConnection dbConnection;
    private static final int MAX_ROWS = 10000; // Safety limit

    public ExecuteQueryTool(InformixConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    /**
     * Execute a SELECT query and return results.
     *
     * @param query SQL SELECT statement
     * @return QueryResult containing column names and row data
     * @throws SecurityException if query validation fails
     * @throws SQLException if query execution fails
     */
    public QueryResult executeQuery(String query) throws SQLException, SecurityException {
        return executeQuery(query, MAX_ROWS);
    }

    /**
     * Execute a SELECT query with a custom row limit.
     *
     * @param query SQL SELECT statement
     * @param maxRows Maximum number of rows to return
     * @return QueryResult containing column names and row data
     * @throws SecurityException if query validation fails
     * @throws SQLException if query execution fails
     */
    public QueryResult executeQuery(String query, int maxRows) throws SQLException, SecurityException {
        // Validate query first
        QueryValidator.validateQuery(query);

        logger.info("Executing query (max rows: {}): {}", maxRows, truncateQuery(query));

        List<String> columnNames = new ArrayList<>();
        List<Map<String, Object>> rows = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Set read-only and max rows
            conn.setReadOnly(true);
            stmt.setMaxRows(maxRows);

            // Set query timeout (30 seconds)
            stmt.setQueryTimeout(30);

            long startTime = System.currentTimeMillis();

            try (ResultSet rs = stmt.executeQuery(query)) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                // Get column names
                for (int i = 1; i <= columnCount; i++) {
                    columnNames.add(metaData.getColumnName(i));
                }

                // Fetch rows
                int rowCount = 0;
                while (rs.next() && rowCount < maxRows) {
                    Map<String, Object> row = new LinkedHashMap<>();

                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = columnNames.get(i - 1);
                        Object value = getColumnValue(rs, i, metaData.getColumnType(i));
                        row.put(columnName, value);
                    }

                    rows.add(row);
                    rowCount++;
                }

                long duration = System.currentTimeMillis() - startTime;
                logger.info("Query executed successfully: {} rows in {} ms", rows.size(), duration);
            }

        } catch (SQLException e) {
            logger.error("Query execution failed: {}", e.getMessage());
            throw e;
        }

        return new QueryResult(columnNames, rows);
    }

    /**
     * Get column value with proper type handling.
     */
    private Object getColumnValue(ResultSet rs, int columnIndex, int sqlType) throws SQLException {
        Object value = rs.getObject(columnIndex);

        if (value == null) {
            return null;
        }

        // Handle specific SQL types
        return switch (sqlType) {
            case Types.DATE -> rs.getDate(columnIndex);
            case Types.TIME -> rs.getTime(columnIndex);
            case Types.TIMESTAMP -> rs.getTimestamp(columnIndex);
            case Types.CLOB -> {
                Clob clob = rs.getClob(columnIndex);
                yield clob != null ? clob.getSubString(1, (int) clob.length()) : null;
            }
            case Types.BLOB -> {
                Blob blob = rs.getBlob(columnIndex);
                yield blob != null ? "[BLOB " + blob.length() + " bytes]" : null;
            }
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY ->
                "[BINARY DATA]";
            default -> value;
        };
    }

    /**
     * Execute query and return results as JSON-friendly map.
     */
    public Map<String, Object> executeQueryAsMap(String query) throws SQLException, SecurityException {
        return executeQueryAsMap(query, MAX_ROWS);
    }

    /**
     * Execute query and return results as JSON-friendly map with custom row limit.
     */
    public Map<String, Object> executeQueryAsMap(String query, int maxRows) throws SQLException, SecurityException {
        QueryResult result = executeQuery(query, maxRows);

        Map<String, Object> response = new HashMap<>();
        response.put("columns", result.columnNames());
        response.put("rows", result.rows());
        response.put("rowCount", result.rowCount());

        if (result.rowCount() >= maxRows) {
            response.put("truncated", true);
            response.put("message", "Results limited to " + maxRows + " rows");
        }

        return response;
    }

    /**
     * Truncate query for logging.
     */
    private String truncateQuery(String query) {
        if (query == null) return "null";
        int maxLength = 200;
        if (query.length() <= maxLength) {
            return query;
        }
        return query.substring(0, maxLength) + "... (truncated)";
    }
}
