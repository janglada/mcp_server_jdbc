package com.informix.mcp;

import com.informix.mcp.database.QueryValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for QueryValidator.
 * Tests various SQL query patterns to ensure security validation works correctly.
 */
class QueryValidatorTest {

    @Test
    void testValidSelectQueries() {
        // Simple SELECT
        assertDoesNotThrow(() -> QueryValidator.validateQuery("SELECT * FROM customers"));

        // SELECT with WHERE clause
        assertDoesNotThrow(() -> QueryValidator.validateQuery("SELECT id, name FROM users WHERE active = true"));

        // SELECT with JOIN
        assertDoesNotThrow(() -> QueryValidator.validateQuery(
            "SELECT c.name, o.total FROM customers c JOIN orders o ON c.id = o.customer_id"
        ));

        // SELECT with subquery
        assertDoesNotThrow(() -> QueryValidator.validateQuery(
            "SELECT * FROM orders WHERE customer_id IN (SELECT id FROM customers WHERE country = 'USA')"
        ));

        // CTE (Common Table Expression)
        assertDoesNotThrow(() -> QueryValidator.validateQuery(
            "WITH active_users AS (SELECT * FROM users WHERE active = true) SELECT * FROM active_users"
        ));

        // SELECT with column containing "UPDATE"
        assertDoesNotThrow(() -> QueryValidator.validateQuery(
            "SELECT id, update_date, create_date FROM products"
        ));
    }

    @Test
    void testInvalidQueries() {
        // UPDATE
        assertThrows(SecurityException.class, () ->
            QueryValidator.validateQuery("UPDATE customers SET name = 'hacker' WHERE id = 1"));

        // INSERT
        assertThrows(SecurityException.class, () ->
            QueryValidator.validateQuery("INSERT INTO customers (name) VALUES ('hacker')"));

        // DELETE
        assertThrows(SecurityException.class, () ->
            QueryValidator.validateQuery("DELETE FROM customers WHERE id = 1"));

        // DROP
        assertThrows(SecurityException.class, () ->
            QueryValidator.validateQuery("DROP TABLE customers"));

        // CREATE
        assertThrows(SecurityException.class, () ->
            QueryValidator.validateQuery("CREATE TABLE hackers (id INT)"));

        // ALTER
        assertThrows(SecurityException.class, () ->
            QueryValidator.validateQuery("ALTER TABLE customers ADD COLUMN evil VARCHAR(100)"));

        // TRUNCATE
        assertThrows(SecurityException.class, () ->
            QueryValidator.validateQuery("TRUNCATE TABLE customers"));

        // EXECUTE PROCEDURE
        assertThrows(SecurityException.class, () ->
            QueryValidator.validateQuery("EXECUTE PROCEDURE update_data()"));
    }

    @Test
    void testMultipleStatements() {
        // Multiple statements with semicolon
        assertThrows(SecurityException.class, () ->
            QueryValidator.validateQuery("SELECT * FROM customers; DELETE FROM customers"));

        // SELECT with trailing semicolon should pass
        assertDoesNotThrow(() ->
            QueryValidator.validateQuery("SELECT * FROM customers;"));
    }

    @Test
    void testNullAndEmptyQueries() {
        // Null query
        assertThrows(SecurityException.class, () ->
            QueryValidator.validateQuery(null));

        // Empty query
        assertThrows(SecurityException.class, () ->
            QueryValidator.validateQuery(""));

        // Whitespace only
        assertThrows(SecurityException.class, () ->
            QueryValidator.validateQuery("   "));
    }

    @Test
    void testQueriesWithComments() {
        // Single-line comment
        assertDoesNotThrow(() ->
            QueryValidator.validateQuery("SELECT * FROM customers -- This is a comment"));

        // Multi-line comment
        assertDoesNotThrow(() ->
            QueryValidator.validateQuery("SELECT * FROM customers /* This is a comment */"));
    }

    @Test
    void testIsValidQuery() {
        assertTrue(QueryValidator.isValidQuery("SELECT * FROM customers"));
        assertFalse(QueryValidator.isValidQuery("UPDATE customers SET name = 'hacker'"));
        assertFalse(QueryValidator.isValidQuery(null));
        assertFalse(QueryValidator.isValidQuery(""));
    }
}
