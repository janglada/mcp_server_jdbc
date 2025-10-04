package com.informix.mcp.model;

import java.util.List;
import java.util.Map;

/**
 * Record representing the result of a SQL query execution.
 * Uses Java 21 record for immutable data structure.
 */
public record QueryResult(
    List<String> columnNames,
    List<Map<String, Object>> rows,
    int rowCount
) {
    public QueryResult(List<String> columnNames, List<Map<String, Object>> rows) {
        this(columnNames, rows, rows.size());
    }
}
