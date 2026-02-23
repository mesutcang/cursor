package com.example.snowflake;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.Security;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;

public class SnowflakeClient {

    private final Properties config;

    public SnowflakeClient(Properties config) {
        this.config = config;
    }

    /**
     * Loads an unencrypted or passphrase-encrypted PKCS#8 / PKCS#1 private key
     * from a PEM file.
     */
    private PrivateKey loadPrivateKey(String keyFilePath) throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        try (PEMParser parser = new PEMParser(new FileReader(keyFilePath))) {
            Object object = parser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

            if (object instanceof PrivateKeyInfo info) {
                return converter.getPrivateKey(info);
            }

            if (object instanceof PEMKeyPair keyPair) {
                return converter.getPrivateKey(keyPair.getPrivateKeyInfo());
            }

            if (object instanceof PEMEncryptedKeyPair encrypted) {
                String passphrase = config.getProperty("private_key_passphrase", "");
                PEMDecryptorProvider decryptor =
                        new JcePEMDecryptorProviderBuilder().build(passphrase.toCharArray());
                PEMKeyPair decryptedPair = encrypted.decryptKeyPair(decryptor);
                return converter.getPrivateKey(decryptedPair.getPrivateKeyInfo());
            }

            throw new IllegalArgumentException(
                    "Unsupported PEM object type: " + object.getClass().getName());
        }
    }

    /**
     * Opens a JDBC connection to Snowflake using private key authentication.
     */
    public Connection connect() throws Exception {
        String account  = config.getProperty("account");
        String user     = config.getProperty("user");
        String database = config.getProperty("database");
        String schema   = config.getProperty("schema");
        String warehouse = config.getProperty("warehouse");
        String role     = config.getProperty("role", "");
        String keyPath  = config.getProperty("private_key_path");

        PrivateKey privateKey = loadPrivateKey(keyPath);

        String url = String.format("jdbc:snowflake://%s.snowflakecomputing.com/", account);

        Properties props = new Properties();
        props.put("user", user);
        props.put("privateKey", privateKey);
        props.put("db", database);
        props.put("schema", schema);
        props.put("warehouse", warehouse);
        if (!role.isEmpty()) {
            props.put("role", role);
        }

        System.out.println("Connecting to Snowflake account: " + account);
        Connection connection = DriverManager.getConnection(url, props);
        System.out.println("Connected successfully.");
        return connection;
    }

    /**
     * Executes a SQL query and prints every row to stdout.
     */
    public void executeQuery(Connection connection, String sql) throws SQLException {
        System.out.println("\nExecuting query:\n  " + sql + "\n");

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            StringBuilder header = new StringBuilder();
            StringBuilder separator = new StringBuilder();
            for (int i = 1; i <= columnCount; i++) {
                String name = meta.getColumnLabel(i);
                if (i > 1) {
                    header.append(" | ");
                    separator.append("-+-");
                }
                header.append(String.format("%-20s", name));
                separator.append("-".repeat(20));
            }
            System.out.println(header);
            System.out.println(separator);

            int rowCount = 0;
            while (rs.next()) {
                StringBuilder row = new StringBuilder();
                for (int i = 1; i <= columnCount; i++) {
                    if (i > 1) {
                        row.append(" | ");
                    }
                    String value = rs.getString(i);
                    row.append(String.format("%-20s", value != null ? value : "NULL"));
                }
                System.out.println(row);
                rowCount++;
            }
            System.out.println("\n" + rowCount + " row(s) returned.");
        }
    }

    /**
     * Loads config from src/main/resources/config.properties (classpath) or a
     * file path passed as the first CLI argument.
     */
    private static Properties loadConfig(String[] args) throws IOException {
        Properties props = new Properties();

        if (args.length > 0 && Files.exists(Path.of(args[0]))) {
            try (FileReader reader = new FileReader(args[0])) {
                props.load(reader);
            }
            System.out.println("Loaded config from file: " + args[0]);
        } else {
            try (InputStream in = SnowflakeClient.class
                    .getClassLoader().getResourceAsStream("config.properties")) {
                if (in == null) {
                    throw new IOException(
                            "config.properties not found on classpath. "
                            + "Copy config.properties.template to "
                            + "src/main/resources/config.properties and fill in your values.");
                }
                props.load(in);
                System.out.println("Loaded config from classpath.");
            }
        }
        return props;
    }

    public static void main(String[] args) {
        try {
            Properties config = loadConfig(args);
            SnowflakeClient client = new SnowflakeClient(config);

            try (Connection conn = client.connect()) {

                // Sample query: Snowflake system function that works without any tables
                client.executeQuery(conn, "SELECT CURRENT_VERSION() AS snowflake_version");

                // Sample query: basic metadata
                client.executeQuery(conn,
                        "SELECT CURRENT_USER() AS current_user, "
                        + "CURRENT_ROLE() AS current_role, "
                        + "CURRENT_DATABASE() AS current_database, "
                        + "CURRENT_SCHEMA() AS current_schema, "
                        + "CURRENT_WAREHOUSE() AS current_warehouse");

                // Sample query: list tables in the current schema
                client.executeQuery(conn,
                        "SHOW TABLES IN SCHEMA " + config.getProperty("database")
                        + "." + config.getProperty("schema"));

                // You can add custom queries here. For example:
                // client.executeQuery(conn, "SELECT * FROM my_table LIMIT 10");
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
