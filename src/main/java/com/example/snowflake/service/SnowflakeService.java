package com.example.snowflake.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SnowflakeService {

    private static final Logger log = LoggerFactory.getLogger(SnowflakeService.class);

    private final DataSource dataSource;

    public SnowflakeService(DataSource dataSource) {
        this.dataSource = dataSource;
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

    public QueryResponse executeQuery(String sql, Object... params) {
        log.info("Executing parameterized query: {}", sql);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {
                return mapResultSet(sql, rs);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Query execution failed: " + e.getMessage(), e);
        }
    }

    public <T> List<T> executeQuery(String sql, RowMapper<T> rowMapper, Object... params) {
        log.info("Executing typed query: {}", sql);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {
                List<T> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(rowMapper.map(rs));
                }
                log.info("Query returned {} row(s)", results.size());
                return results;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Query execution failed: " + e.getMessage(), e);
        }
    }

    public EprivacyConsentResponse findEprivacyConsentByVin(String vin) {
        String sql = "SELECT * FROM ACL_EPRIVACY_CONSENT.EPRIVACY_CONSENT WHERE VIN = ?";
        log.info("Executing eprivacy consent query for VIN: {}", vin);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, vin);

            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();

                List<String> columns = new ArrayList<>(columnCount);
                for (int i = 1; i <= columnCount; i++) {
                    columns.add(meta.getColumnLabel(i));
                }

                List<EprivacyConsent> consents = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> fields = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        fields.put(columns.get(i - 1), rs.getObject(i));
                    }
                    consents.add(new EprivacyConsent(fields));
                }

                log.info("Query returned {} row(s) for VIN: {}", consents.size(), vin);
                return new EprivacyConsentResponse(vin, columns, consents, consents.size());
            }

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
