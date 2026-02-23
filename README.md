# Java Snowflake Client (Spring Boot)

A Spring Boot REST API that connects to Snowflake using **private key pair authentication** and exposes query endpoints.

## Prerequisites

- **Java 21+**
- A Snowflake account with key pair authentication configured for your user
  ([Snowflake docs](https://docs.snowflake.com/en/user-guide/key-pair-auth))

## Generate a Key Pair (if you don't have one)

```bash
# Generate an unencrypted PKCS#8 private key
openssl genrsa 2048 | openssl pkcs8 -topk8 -inform PEM -out rsa_key.p8 -nocrypt

# Extract the public key
openssl rsa -in rsa_key.p8 -pubout -out rsa_key.pub
```

Then assign the public key to your Snowflake user:

```sql
ALTER USER your_user SET RSA_PUBLIC_KEY='<paste public key without header/footer>';
```

## Configuration

All settings live in `src/main/resources/application.yml` and can be overridden with environment variables:

| Property                        | Env Variable                      | Description                                              |
|---------------------------------|-----------------------------------|----------------------------------------------------------|
| `snowflake.account`             | `SNOWFLAKE_ACCOUNT`               | Snowflake account identifier (e.g. `xy12345.us-east-1`)  |
| `snowflake.user`                | `SNOWFLAKE_USER`                  | Snowflake login user                                     |
| `snowflake.database`            | `SNOWFLAKE_DATABASE`              | Default database                                         |
| `snowflake.schema`              | `SNOWFLAKE_SCHEMA`                | Default schema (defaults to `PUBLIC`)                    |
| `snowflake.warehouse`           | `SNOWFLAKE_WAREHOUSE`             | Virtual warehouse                                        |
| `snowflake.role`                | `SNOWFLAKE_ROLE`                  | Role (optional)                                          |
| `snowflake.private-key-path`    | `SNOWFLAKE_PRIVATE_KEY_PATH`      | Absolute path to the PEM private key file                |
| `snowflake.private-key-passphrase` | `SNOWFLAKE_PRIVATE_KEY_PASSPHRASE` | Passphrase (leave blank if key is unencrypted)         |

### Quick start with environment variables

```bash
export SNOWFLAKE_ACCOUNT=xy12345.us-east-1
export SNOWFLAKE_USER=my_user
export SNOWFLAKE_DATABASE=my_db
export SNOWFLAKE_WAREHOUSE=my_wh
export SNOWFLAKE_PRIVATE_KEY_PATH=/path/to/rsa_key.p8
```

## Build

```bash
./gradlew build -x test
```

## Run

```bash
./gradlew bootRun
```

Or run the fat JAR directly:

```bash
java -jar build/libs/snowflake-client-1.0.0.jar
```

The server starts on **http://localhost:8080**.

## REST API Endpoints

### GET /api/snowflake/version

Returns the current Snowflake server version.

```bash
curl http://localhost:8080/api/snowflake/version
```

```json
{
  "sql": "SELECT CURRENT_VERSION() AS snowflake_version",
  "columns": ["snowflake_version"],
  "rows": [{ "snowflake_version": "8.30.1" }],
  "rowCount": 1
}
```

### GET /api/snowflake/current-session

Returns metadata about the current session (user, role, database, schema, warehouse).

```bash
curl http://localhost:8080/api/snowflake/current-session
```

### GET /api/snowflake/tables?database=MY_DB&schema=PUBLIC

Lists tables in the specified database and schema.

```bash
curl "http://localhost:8080/api/snowflake/tables?database=MY_DB&schema=PUBLIC"
```

### POST /api/snowflake/query

Execute an arbitrary SQL query.

```bash
curl -X POST http://localhost:8080/api/snowflake/query \
  -H "Content-Type: application/json" \
  -d '{"sql": "SELECT * FROM my_table LIMIT 10"}'
```

```json
{
  "sql": "SELECT * FROM my_table LIMIT 10",
  "columns": ["id", "name", "created_at"],
  "rows": [
    { "id": 1, "name": "Alice", "created_at": "2025-01-01" }
  ],
  "rowCount": 1
}
```

## Project Structure

```
├── build.gradle                 # Gradle build with Spring Boot & Snowflake JDBC
├── settings.gradle
├── gradlew / gradlew.bat        # Gradle wrapper
└── src/main/
    ├── java/com/example/snowflake/
    │   ├── SnowflakeClientApplication.java      # Spring Boot entry point
    │   ├── config/
    │   │   ├── SnowflakeConfig.java             # DataSource bean with private key auth
    │   │   └── SnowflakeProperties.java         # Typed config properties
    │   ├── controller/
    │   │   ├── SnowflakeController.java         # REST endpoints
    │   │   └── GlobalExceptionHandler.java      # Error handling
    │   ├── model/
    │   │   ├── QueryRequest.java                # Request body DTO
    │   │   └── QueryResponse.java               # Response DTO
    │   └── service/
    │       └── SnowflakeService.java            # Query execution logic
    └── resources/
        └── application.yml                      # App & Snowflake configuration
```
