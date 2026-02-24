package com.example.snowflake.config;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;

public class SnowflakeDialect extends Dialect {

    public SnowflakeDialect() {
        super(DatabaseVersion.make(8, 0));
    }
}
