# Java Snowflake Client

A Java client that connects to Snowflake using **private key pair authentication** (key pair auth), executes sample queries, and prints the results.

## Prerequisites

- **Java 21+**
- **Maven 3.8+**
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

Copy the template and fill in your connection details:

```bash
cp src/main/resources/config.properties.template src/main/resources/config.properties
```

Edit `src/main/resources/config.properties`:

| Property                 | Description                                        |
|--------------------------|----------------------------------------------------|
| `account`                | Snowflake account identifier (e.g. `xy12345.us-east-1`) |
| `user`                   | Snowflake login user                               |
| `database`               | Default database                                   |
| `schema`                 | Default schema                                     |
| `warehouse`              | Virtual warehouse                                  |
| `role`                   | Role (optional)                                    |
| `private_key_path`       | Absolute path to the PEM private key file          |
| `private_key_passphrase` | Passphrase (leave blank if key is unencrypted)     |

## Build

```bash
mvn clean package -q
```

This produces a fat JAR in `target/snowflake-client-1.0.0.jar`.

## Run

Using the classpath config:

```bash
java -jar target/snowflake-client-1.0.0.jar
```

Or pass an external config file:

```bash
java -jar target/snowflake-client-1.0.0.jar /path/to/config.properties
```

## Sample Output

```
Loaded config from classpath.
Connecting to Snowflake account: xy12345.us-east-1
Connected successfully.

Executing query:
  SELECT CURRENT_VERSION() AS snowflake_version

SNOWFLAKE_VERSION
--------------------
8.30.1

1 row(s) returned.
```

## Adding Custom Queries

Open `SnowflakeClient.java` and add calls to `client.executeQuery(conn, "YOUR SQL HERE")` inside the `main` method.
