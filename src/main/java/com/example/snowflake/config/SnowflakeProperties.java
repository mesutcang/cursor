package com.example.snowflake.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "snowflake")
public record SnowflakeProperties(
        String account,
        String user,
        String database,
        String schema,
        String warehouse,
        String role,
        String privateKeyPath,
        String privateKeyPassphrase
) {}
