package com.example.snowflake.service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.example.snowflake.model.EprivacyConsent;
import com.example.snowflake.model.EprivacyConsentResponse;
import com.example.snowflake.model.QueryResponse;
import com.example.snowflake.repository.EprivacyConsentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SnowflakeService {

    private static final Logger log = LoggerFactory.getLogger(SnowflakeService.class);

    private final DataSource dataSource;
    private final EprivacyConsentRepository eprivacyConsentRepository;

    public SnowflakeService(DataSource dataSource,
                            EprivacyConsentRepository eprivacyConsentRepository) {
        this.dataSource = dataSource;
        this.eprivacyConsentRepository = eprivacyConsentRepository;
    }

    public EprivacyConsentResponse findEprivacyConsentByVin(String vin) {
        log.info("Looking up e-privacy consent for VIN: {}", vin);
        List<EprivacyConsent> consents = eprivacyConsentRepository.findByVin(vin);
        log.info("Found {} record(s) for VIN: {}", consents.size(), vin);
        return new EprivacyConsentResponse(vin, consents, consents.size());
    }

    public QueryResponse executeQuery(String sql) {
        log.info("Executing query: {}", sql);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            return mapResultSet(sql, rs);

        } catch (SQLException e) {
            throw new RuntimeException("Query execution failed: " + e.getMessage(), e);
        }
    }

    private QueryResponse mapResultSet(String sql, ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();

        List<String> columns = new ArrayList<>(columnCount);
        for (int i = 1; i <= columnCount; i++) {
            columns.add(meta.getColumnLabel(i));
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                row.put(columns.get(i - 1), rs.getObject(i));
            }
            rows.add(row);
        }

        log.info("Query returned {} row(s)", rows.size());
        return new QueryResponse(sql, columns, rows, rows.size());
    }
}
