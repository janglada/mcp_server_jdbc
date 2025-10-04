package com.informix.mcp.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Validates SQL queries to ensure only SELECT statements are executed.
 * Prevents SQL injection and unauthorized data modification.
 */
public class QueryValidator {
    private static final Logger logger = LoggerFactory.getLogger(QueryValidator.class);

    // Dangerous SQL keywords that should never appear in a SELECT query
    private static final String[] FORBIDDEN_KEYWORDS = {
        "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER", "TRUNCATE",
        "GRANT", "REVOKE", "EXEC", "EXECUTE", "CALL", "MERGE", "REPLACE",
        "RENAME", "COMMENT", "LOAD", "UNLOAD", "COMMIT", "ROLLBACK",
        "SAVEPOINT", "SET TRANSACTION", "START TRANSACTION", "BEGIN WORK",
        "LOCK", "UNLOCK"
    };

    // Pattern to detect SQL comments that might hide malicious code
    private static final Pattern SQL_COMMENT_PATTERN = Pattern.compile(
        "--.*?$|/\\*.*?\\*/|#.*?$",
        Pattern.MULTILINE | Pattern.DOTALL
    );

    // Pattern to detect multiple statements (semicolons)
    private static final Pattern SEMICOLON_PATTERN = Pattern.compile(
        ";\\s*(?!\\s*$)",  // Semicolons not followed by only whitespace/end
        Pattern.MULTILINE
    );

    // Pattern to validate SELECT statement start
    private static final Pattern SELECT_START_PATTERN = Pattern.compile(
        "^\\s*(WITH\\s+.*?\\s+)?SELECT\\s+",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    /**
     * Validates a SQL query to ensure it's a safe SELECT statement.
     *
     * @param query The SQL query to validate
     * @throws SecurityException if the query is not a valid SELECT statement
     */
    public static void validateQuery(String query) throws SecurityException {
        if (query == null || query.trim().isEmpty()) {
            throw new SecurityException("Query cannot be null or empty");
        }

        String normalizedQuery = query.trim();

        // Check 1: Detect multiple statements
        if (SEMICOLON_PATTERN.matcher(normalizedQuery).find()) {
            logger.warn("Rejected query with multiple statements: {}", truncateQuery(query));
            throw new SecurityException("Multiple statements are not allowed. Only single SELECT queries are permitted.");
        }

        // Check 2: Remove comments (but keep track if they existed)
        String queryWithoutComments = SQL_COMMENT_PATTERN.matcher(normalizedQuery).replaceAll(" ");
        boolean hadComments = !queryWithoutComments.equals(normalizedQuery);

        // Use the query without comments for validation
        String validationQuery = queryWithoutComments.trim();

        // Check 3: Must start with SELECT (or WITH...SELECT for CTEs)
        if (!SELECT_START_PATTERN.matcher(validationQuery).find()) {
            logger.warn("Rejected non-SELECT query: {}", truncateQuery(query));
            throw new SecurityException("Only SELECT queries are allowed. Query must start with SELECT or WITH...SELECT.");
        }

        // Check 4: Check for forbidden keywords
        String upperQuery = validationQuery.toUpperCase();
        for (String keyword : FORBIDDEN_KEYWORDS) {
            // Use word boundaries to avoid false positives (e.g., "UPDATE_DATE" column name)
            Pattern keywordPattern = Pattern.compile("\\b" + keyword + "\\b", Pattern.CASE_INSENSITIVE);
            if (keywordPattern.matcher(validationQuery).find()) {
                logger.warn("Rejected query with forbidden keyword '{}': {}", keyword, truncateQuery(query));
                throw new SecurityException(
                    String.format("Forbidden SQL keyword detected: %s. Only SELECT queries are allowed.", keyword)
                );
            }
        }

        // Check 5: Warn if comments were removed (potential obfuscation attempt)
        if (hadComments) {
            logger.info("Query contained comments (removed for validation): {}", truncateQuery(query));
        }

        // Check 6: Additional validation for specific Informix system procedures that might modify data
        if (upperQuery.contains("EXECUTE PROCEDURE") || upperQuery.contains("EXECUTE FUNCTION")) {
            logger.warn("Rejected query attempting to execute procedure/function: {}", truncateQuery(query));
            throw new SecurityException("Executing procedures or functions is not allowed.");
        }

        logger.debug("Query validation passed: {}", truncateQuery(query));
    }

    /**
     * Checks if a query is valid without throwing an exception.
     *
     * @param query The SQL query to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidQuery(String query) {
        try {
            validateQuery(query);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    /**
     * Truncate query for logging purposes to avoid logging sensitive data.
     */
    private static String truncateQuery(String query) {
        if (query == null) return "null";
        int maxLength = 100;
        if (query.length() <= maxLength) {
            return query;
        }
        return query.substring(0, maxLength) + "... (truncated)";
    }
}
