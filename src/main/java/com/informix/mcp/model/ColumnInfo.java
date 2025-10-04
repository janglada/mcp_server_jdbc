package com.informix.mcp.model;

/**
 * Record representing database column metadata.
 * Uses Java 21 record for immutable data structure.
 */
public record ColumnInfo(
    String columnName,
    String dataType,
    int columnSize,
    boolean isNullable,
    boolean isPrimaryKey,
    String remarks
) {
    @Override
    public String toString() {
        return String.format("%s %s(%d) %s %s",
            columnName,
            dataType,
            columnSize,
            isNullable ? "NULL" : "NOT NULL",
            isPrimaryKey ? "PRIMARY KEY" : ""
        ).trim();
    }
}
