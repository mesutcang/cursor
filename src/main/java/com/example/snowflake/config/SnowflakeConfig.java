package com.example.snowflake.config;

import java.io.FileReader;
import java.security.PrivateKey;
import java.security.Security;
import java.util.Properties;

import javax.sql.DataSource;

import net.snowflake.client.jdbc.SnowflakeBasicDataSource;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SnowflakeProperties.class)
public class SnowflakeConfig {

    private final SnowflakeProperties props;

    public SnowflakeConfig(SnowflakeProperties props) {
        this.props = props;
    }

    @Bean
    public DataSource snowflakeDataSource() throws Exception {
        PrivateKey privateKey = loadPrivateKey(
                props.privateKeyPath(),
                props.privateKeyPassphrase()
        );

        SnowflakeBasicDataSource ds = new SnowflakeBasicDataSource();
        ds.setUrl(String.format("jdbc:snowflake://%s.snowflakecomputing.com/", props.account()));
        ds.setUser(props.user());
        ds.setPrivateKey(privateKey);
        ds.setDatabaseName(props.database());
        ds.setSchema(props.schema());
        ds.setWarehouse(props.warehouse());
        if (props.role() != null && !props.role().isBlank()) {
            ds.setRole(props.role());
        }

        return ds;
    }

    /**
     * Loads an unencrypted or passphrase-encrypted PKCS#8 / PKCS#1 private key
     * from a PEM file.
     */
    private PrivateKey loadPrivateKey(String keyFilePath, String passphrase) throws Exception {
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
                String pw = (passphrase != null) ? passphrase : "";
                PEMDecryptorProvider decryptor =
                        new JcePEMDecryptorProviderBuilder().build(pw.toCharArray());
                PEMKeyPair decryptedPair = encrypted.decryptKeyPair(decryptor);
                return converter.getPrivateKey(decryptedPair.getPrivateKeyInfo());
            }

            throw new IllegalArgumentException(
                    "Unsupported PEM object type: " + object.getClass().getName());
        }
    }
}
