# Informix MCP Server

A Model Context Protocol (MCP) server implementation in Java 21 using the official MCP Java SDK that provides read-only database access to IBM Informix databases via JDBC with embedded Jetty HTTP transport.

## Features

- **Official MCP SDK**: Built with `io.modelcontextprotocol.sdk:mcp` (v0.14.0)
- **Embedded Jetty**: Lightweight HTTP server without Spring framework
- **HTTP/Servlet Transport**: RESTful HTTP endpoint at `/mcp/message`
- **No Spring**: Pure Java with Jetty - minimal dependencies
- **Read-Only Access**: Secure, read-only database operations
- **Three Core Tools**:
  - `list_tables`: List all tables in the database
  - `get_table_schema`: Get detailed schema information for a table
  - `execute_query`: Execute SELECT queries with strict validation
- **Security**:
  - SQL injection prevention
  - Query validation (only SELECT statements allowed)
  - Connection pooling with HikariCP
- **Java 21 Features**:
  - Records for immutable data structures
  - Pattern matching
  - Text blocks

## Quick Start

### With Docker (Easiest)

```bash
# 1. Start Informix Docker container
docker run -d --name informix -p 9088:9088 -e LICENSE=accept \
  ibmcom/informix-developer-database:latest

# 2. Wait ~30 seconds for Informix to initialize, then build and run
mvn clean package -DskipTests
java -jar target/mcp-server-jdbc-1.0.0-jar-with-dependencies.jar

# 3. Server starts on http://localhost:8080/mcp/message
# Test it:
curl -X POST http://localhost:8080/mcp/message \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}'
```

The MCP server will automatically connect to the Informix Docker container using default settings.

## Requirements

- Java Development Kit (JDK) 21 or higher
- Maven 3.6 or higher
- IBM Informix database server (Docker recommended)
- Informix JDBC driver (included in dependencies)

## Project Structure

```
mcp_server_jdbc/
├── src/main/java/com/informix/mcp/
│   ├── InformixMCPServer.java            # Main entry point
│   ├── JettyServerConfig.java            # Jetty and MCP configuration
│   ├── config/
│   │   └── DatabaseConfig.java           # Configuration management
│   ├── database/
│   │   ├── InformixConnection.java       # Connection pool manager
│   │   └── QueryValidator.java           # SQL query validation
│   ├── model/
│   │   ├── ColumnInfo.java               # Column metadata record
│   │   └── QueryResult.java              # Query result record
│   └── tools/
│       ├── ListTablesTool.java           # Database operations
│       ├── ListTablesToolHandler.java    # MCP tool handler
│       ├── GetTableSchemaTool.java       # Database operations
│       ├── GetTableSchemaToolHandler.java # MCP tool handler
│       ├── ExecuteQueryTool.java         # Database operations
│       └── ExecuteQueryToolHandler.java  # MCP tool handler
├── src/main/resources/
│   ├── database.properties.template      # Database config template
│   └── logback.xml                       # Logging configuration
├── pom.xml                               # Maven build configuration
└── README.md                             # This file
```

## Architecture

The server uses the **official MCP Java SDK** with:
- **Embedded Jetty**: Lightweight HTTP server (no Spring, no Tomcat)
- **MCP Servlet Transport**: HTTP-based communication via `/mcp/message` endpoint
- **Tool Handlers**: Each tool has a dedicated handler implementing MCP's `CallToolResult` interface
- **Synchronous API**: Uses `McpServer.sync()` for blocking operations
- **Minimal Dependencies**: Only Jetty, MCP SDK, and necessary libraries

## Installation

### 1. Start Informix Docker Container (Recommended)

The easiest way to get started is using the official Informix Docker container:

```bash
docker run -it --name iif_developer_edition \
  --privileged \
  -p 9088:9088 \
  -p 9089:9089 \
  -e LICENSE=accept \
  ibmcom/informix-developer-database:latest
```

**Default Docker Container Settings:**
- Server Name: `informix`
- Database: `sysmaster` (system database)
- Port: `9088` (SQLI), `9089` (DRDA)
- Username: `informix`
- Password: `in4mix`
- Host: `localhost` (when running outside Docker)

Wait for the message "Informix container login Information" before connecting.

### 2. Clone or Download the Project

```bash
cd /home/joan/workspace/mcp_server_jdbc
```

### 3. Configure Database Connection

**For Docker (Default):** No configuration needed! The defaults match the Docker container.

**For Custom Setup:**

Choose one of the following configuration methods:

#### Option A: Using Properties File

```bash
cp src/main/resources/database.properties.template database.properties
```

Edit `database.properties`:

```properties
# Defaults for Informix Docker container
informix.host=localhost
informix.port=9088
informix.database=sysmaster
informix.server=informix
informix.username=informix
informix.password=in4mix
informix.pool.size=5
```

Then set the environment variable:

```bash
export INFORMIX_CONFIG_FILE=/path/to/database.properties
```

#### Option B: Using Environment Variables

```bash
cp .env.template .env
```

Edit `.env` and source it:

```bash
source .env
```

Or set variables directly:

```bash
# Defaults for Informix Docker container
export INFORMIX_HOST=localhost
export INFORMIX_PORT=9088
export INFORMIX_DATABASE=sysmaster
export INFORMIX_SERVER=informix
export INFORMIX_USERNAME=informix
export INFORMIX_PASSWORD=in4mix
export INFORMIX_POOL_SIZE=5
```

### 4. Build the Project

```bash
mvn clean package
```

This will create:
- `target/mcp-server-jdbc-1.0.0.jar` (standard JAR)
- `target/mcp-server-jdbc-1.0.0-jar-with-dependencies.jar` (standalone JAR with all dependencies)

## Running the Server

### Standalone Mode (Default Port 8080)

```bash
java -jar target/mcp-server-jdbc-1.0.0-jar-with-dependencies.jar
```

The server will start on `http://localhost:8080` with the MCP endpoint at `/mcp/message`.

### Custom Port (Command Line)

```bash
java -jar target/mcp-server-jdbc-1.0.0-jar-with-dependencies.jar 9090
```

### Custom Port (Environment Variable)

```bash
MCP_PORT=9090 java -jar target/mcp-server-jdbc-1.0.0-jar-with-dependencies.jar
```

### With Configuration File

```bash
INFORMIX_CONFIG_FILE=/path/to/database.properties \
java -jar target/mcp-server-jdbc-1.0.0-jar-with-dependencies.jar
```

### Development Mode

```bash
mvn exec:java -Dexec.mainClass="com.informix.mcp.InformixMCPServer"
```

## MCP Endpoint

The server exposes an MCP endpoint at:

```
POST http://localhost:8080/mcp/message
```

This endpoint implements the Model Context Protocol over HTTP using the official MCP Servlet transport.

## Tool Usage

The server exposes three tools via the MCP protocol. All requests should be sent as JSON to the `/mcp/message` endpoint.

### 1. list_tables

Lists all tables in the database.

**Parameters:**
- `schema` (optional): Filter tables by schema name

**Example Request:**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/call",
  "params": {
    "name": "list_tables",
    "arguments": {
      "schema": "informix"
    }
  }
}
```

**Example Response:**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "{\"tables\": [{\"name\": \"customers\", \"schema\": \"informix\", \"type\": \"TABLE\"}], \"count\": 1}"
      }
    ]
  }
}
```

### 2. get_table_schema

Returns schema information for a specific table.

**Parameters:**
- `table_name` (required): Name of the table (can include schema: `schema.table`)

**Example Request:**
```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/call",
  "params": {
    "name": "get_table_schema",
    "arguments": {
      "table_name": "customers"
    }
  }
}
```

**Example Response:**
```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "{\"tableName\": \"customers\", \"columnCount\": 3, \"columns\": [{\"name\": \"id\", \"type\": \"INTEGER\", \"size\": 10, \"nullable\": false, \"primaryKey\": true}]}"
      }
    ]
  }
}
```

### 3. execute_query

Executes a SELECT query against the database.

**Parameters:**
- `query` (required): SQL SELECT statement
- `max_rows` (optional): Maximum number of rows to return (default: 10000)

**Example Request:**
```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "tools/call",
  "params": {
    "name": "execute_query",
    "arguments": {
      "query": "SELECT * FROM customers WHERE country = 'USA'",
      "max_rows": 100
    }
  }
}
```

**Example Response:**
```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "{\"columns\": [\"id\", \"name\", \"country\"], \"rows\": [{\"id\": 1, \"name\": \"John\", \"country\": \"USA\"}], \"rowCount\": 1}"
      }
    ]
  }
}
```

## Testing with cURL

### Initialize Connection
```bash
curl -X POST http://localhost:8080/mcp/message \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "initialize",
    "params": {}
  }'
```

### List Tools
```bash
curl -X POST http://localhost:8080/mcp/message \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/list",
    "params": {}
  }'
```

### List Tables
```bash
curl -X POST http://localhost:8080/mcp/message \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 3,
    "method": "tools/call",
    "params": {
      "name": "list_tables",
      "arguments": {}
    }
  }'
```

### Get Table Schema
```bash
curl -X POST http://localhost:8080/mcp/message \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 4,
    "method": "tools/call",
    "params": {
      "name": "get_table_schema",
      "arguments": {
        "table_name": "customers"
      }
    }
  }'
```

### Execute Query
```bash
curl -X POST http://localhost:8080/mcp/message \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 5,
    "method": "tools/call",
    "params": {
      "name": "execute_query",
      "arguments": {
        "query": "SELECT * FROM customers LIMIT 10"
      }
    }
  }'
```

## Security Features

### Query Validation

The server implements strict query validation:

- ✅ **Allowed**: SELECT queries, CTEs (WITH...SELECT)
- ❌ **Blocked**: INSERT, UPDATE, DELETE, DROP, ALTER, CREATE, TRUNCATE, EXEC, and other modification statements
- ❌ **Blocked**: Multiple statements (semicolon-separated)
- ❌ **Blocked**: SQL comments that might hide malicious code
- ❌ **Blocked**: Stored procedure execution

### Example of Blocked Queries

```sql
-- ❌ Blocked: Data modification
UPDATE customers SET name = 'hacker'

-- ❌ Blocked: Table deletion
DROP TABLE customers

-- ❌ Blocked: Multiple statements
SELECT * FROM customers; DELETE FROM customers

-- ❌ Blocked: Procedure execution
EXECUTE PROCEDURE update_data()

-- ✅ Allowed: Simple SELECT
SELECT * FROM customers WHERE id = 1

-- ✅ Allowed: CTE
WITH active_customers AS (
  SELECT * FROM customers WHERE active = true
)
SELECT * FROM active_customers
```

## Connection Pooling

The server uses HikariCP for connection pooling with the following default settings:

- Maximum pool size: 5 connections (configurable)
- Minimum idle: 1 connection
- Connection timeout: 30 seconds
- Idle timeout: 10 minutes
- Max lifetime: 30 minutes
- Connection test query: `SELECT 1 FROM systables WHERE tabid=1`

## Logging

Logs are written to console (stdout/stderr) and optionally to file.

Log levels can be adjusted in `src/main/resources/logback.xml`.

## Configuration

### Server Configuration

- **Port**: Default 8080
  - Override via command line: `java -jar server.jar 9090`
  - Override via environment: `MCP_PORT=9090 java -jar server.jar`

### Database Configuration

Environment variables (highest priority):
- `INFORMIX_HOST`
- `INFORMIX_PORT`
- `INFORMIX_DATABASE`
- `INFORMIX_SERVER`
- `INFORMIX_USERNAME`
- `INFORMIX_PASSWORD`
- `INFORMIX_POOL_SIZE`

Or use `INFORMIX_CONFIG_FILE` to point to a properties file.

## Testing

### Unit Tests

```bash
mvn test
```

### Integration Tests

To run integration tests, you need a running Informix database:

```bash
mvn verify
```

## Troubleshooting

### Connection Issues

1. **Verify Informix server is running**:
   ```bash
   onstat -
   ```

2. **Check JDBC URL format**:
   ```
   jdbc:informix-sqli://host:port/database:INFORMIXSERVER=server_name
   ```

3. **Test connectivity**:
   ```bash
   curl http://localhost:8080/mcp/message
   ```

### Query Validation Errors

If you get "Forbidden SQL keyword" errors for legitimate SELECT queries:
- Ensure there are no SQL comments in your query
- Check for keywords like "UPDATE_DATE" (use quotes: `"UPDATE_DATE"`)
- Verify the query doesn't contain semicolons

### Port Already in Use

Change the port:
```bash
java -jar target/mcp-server-jdbc-1.0.0-jar-with-dependencies.jar 9090
```

Or:
```bash
MCP_PORT=9090 java -jar target/mcp-server-jdbc-1.0.0-jar-with-dependencies.jar
```

## Development

### Building from Source

```bash
mvn clean compile
```

### Running Tests

```bash
mvn test
```

### Creating a Release

```bash
mvn clean package -DskipTests
```

### Debugging

Enable DEBUG logging in `logback.xml`:

```xml
<logger name="com.informix.mcp" level="DEBUG"/>
<logger name="io.modelcontextprotocol" level="DEBUG"/>
<logger name="org.eclipse.jetty" level="INFO"/>
```

## Dependencies

- **MCP SDK** (0.14.0): Official Model Context Protocol Java SDK
- **MCP Servlet** (0.14.0): HTTP Servlet transport for MCP
- **Embedded Jetty** (11.0.24): Lightweight HTTP server and servlet container
- **Informix JDBC Driver** (4.50.10): Official IBM Informix JDBC driver
- **HikariCP** (6.2.1): High-performance JDBC connection pool
- **Jackson** (2.18.2): JSON processing
- **SLF4J/Logback** (2.0.16/1.5.12): Logging framework
- **JUnit 5** (5.11.4): Testing framework

## MCP Protocol Support

This server implements the Model Context Protocol specification:
- Protocol Version: 2024-11-05
- Transport: HTTP/Servlet
- Capabilities: Tools
- Communication: JSON-RPC 2.0

# Using in Claude
```bash
 claude mcp add --transport http database-inspector http://localhost:9090/mcp
```

## Deployment Options

### 1. Standalone JAR
```bash
java -jar target/mcp-server-jdbc-1.0.0-jar-with-dependencies.jar
```

### 2. Behind Reverse Proxy (nginx)

```nginx
server {
    listen 80;
    server_name mcp.example.com;

    location /mcp {
        proxy_pass http://localhost:8080/mcp;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 3. Docker

```dockerfile
FROM eclipse-temurin:21-jre
COPY target/mcp-server-jdbc-1.0.0-jar-with-dependencies.jar /app.jar
EXPOSE 8080
ENV MCP_PORT=8080
CMD ["java", "-jar", "/app.jar"]
```

Build and run:
```bash
docker build -t informix-mcp-server .
docker run -p 8080:8080 \
  -e INFORMIX_HOST=host.docker.internal \
  -e INFORMIX_DATABASE=testdb \
  -e INFORMIX_SERVER=ol_informix1170 \
  -e INFORMIX_USERNAME=informix \
  -e INFORMIX_PASSWORD=secret \
  informix-mcp-server
```

### 4. System Service (systemd)

Create `/etc/systemd/system/informix-mcp.service`:

```ini
[Unit]
Description=Informix MCP Server
After=network.target

[Service]
Type=simple
User=mcp
WorkingDirectory=/opt/informix-mcp
Environment="INFORMIX_HOST=localhost"
Environment="INFORMIX_DATABASE=testdb"
Environment="INFORMIX_SERVER=ol_informix1170"
Environment="INFORMIX_USERNAME=informix"
Environment="INFORMIX_PASSWORD=secret"
Environment="MCP_PORT=8080"
ExecStart=/usr/bin/java -jar /opt/informix-mcp/mcp-server-jdbc-1.0.0-jar-with-dependencies.jar
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

Enable and start:
```bash
sudo systemctl enable informix-mcp
sudo systemctl start informix-mcp
sudo systemctl status informix-mcp
```

## Advantages of Jetty (No Spring)

1. **Lightweight**: ~2MB vs ~30MB+ for Spring Boot
2. **Fast Startup**: Starts in seconds, not tens of seconds
3. **Simple Configuration**: Pure Java, no XML, no annotations magic
4. **Low Memory**: Minimal footprint for production
5. **Full Control**: Direct access to server configuration
6. **HTTP Support**: Standard web server features without framework overhead

## License

This project is provided as-is for use with IBM Informix databases.

## Support

For issues and questions:
1. Check the logs (console output or `logs/mcp-server.log`)
2. Verify database connectivity
3. Review the configuration settings
4. Enable DEBUG logging in `logback.xml`
5. Test the endpoint with cURL

## Contributing

Contributions are welcome! Please ensure:
- Code follows Java best practices
- All tests pass
- Security features are maintained
- Documentation is updated
- MCP protocol compatibility is preserved

## Version History

- **1.0.0** (2025-10-04): Initial release with MCP SDK
  - Official MCP Java SDK integration
  - Embedded Jetty (no Spring, no frameworks)
  - HTTP/Servlet transport
  - Three core tools: list_tables, get_table_schema, execute_query
  - HikariCP connection pooling
  - Comprehensive query validation
  - JDK 21 features (records, pattern matching)

## References

- [Model Context Protocol](https://modelcontextprotocol.io/)
- [MCP Java SDK](https://modelcontextprotocol.io/sdk/java/mcp-overview)
- [MCP Server Guide](https://modelcontextprotocol.io/sdk/java/mcp-server)
- [Eclipse Jetty](https://eclipse.dev/jetty/)
- [IBM Informix Documentation](https://www.ibm.com/docs/en/informix-servers/)
