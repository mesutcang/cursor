package com.example.snowflake.service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import javax.sql.DataSource;

import com.example.snowflake.model.EprivacyConsent;
import com.example.snowflake.model.EprivacyConsentResponse;
import com.example.snowflake.model.QueryResponse;
import com.example.snowflake.repository.EprivacyConsentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SnowflakeServiceTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private EprivacyConsentRepository eprivacyConsentRepository;

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @Mock
    private ResultSet resultSet;

    @Mock
    private ResultSetMetaData resultSetMetaData;

    private SnowflakeService snowflakeService;

    @BeforeEach
    void setUp() {
        snowflakeService = new SnowflakeService(dataSource, eprivacyConsentRepository);
    }

    @Test
    void findEprivacyConsentByVin_returnsConsents_whenRecordsExist() {
        String vin = "WBA12345678901234";
        EprivacyConsent consent = buildConsent("DS-001", "PUR-001", vin, "Marketing", "ACTIVE");

        when(eprivacyConsentRepository.findByVin(vin)).thenReturn(List.of(consent));

        EprivacyConsentResponse response = snowflakeService.findEprivacyConsentByVin(vin);

        assertThat(response.vin()).isEqualTo(vin);
        assertThat(response.totalRecords()).isEqualTo(1);
        assertThat(response.consents()).hasSize(1);
        assertThat(response.consents().get(0).getVin()).isEqualTo(vin);
        assertThat(response.consents().get(0).getPurposeName()).isEqualTo("Marketing");
        assertThat(response.consents().get(0).getPurposeStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void findEprivacyConsentByVin_returnsEmpty_whenNoRecords() {
        String vin = "NONEXISTENT";

        when(eprivacyConsentRepository.findByVin(vin)).thenReturn(List.of());

        EprivacyConsentResponse response = snowflakeService.findEprivacyConsentByVin(vin);

        assertThat(response.vin()).isEqualTo(vin);
        assertThat(response.totalRecords()).isZero();
        assertThat(response.consents()).isEmpty();
    }

    @Test
    void findEprivacyConsentByVin_returnsMultipleConsents() {
        String vin = "WBA12345678901234";
        EprivacyConsent consent1 = buildConsent("DS-001", "PUR-001", vin, "Marketing", "ACTIVE");
        EprivacyConsent consent2 = buildConsent("DS-001", "PUR-002", vin, "Telemetry", "REVOKED");

        when(eprivacyConsentRepository.findByVin(vin)).thenReturn(List.of(consent1, consent2));

        EprivacyConsentResponse response = snowflakeService.findEprivacyConsentByVin(vin);

        assertThat(response.totalRecords()).isEqualTo(2);
        assertThat(response.consents()).extracting(EprivacyConsent::getPurposeName)
                .containsExactly("Marketing", "Telemetry");
        assertThat(response.consents()).extracting(EprivacyConsent::getPurposeStatus)
                .containsExactly("ACTIVE", "REVOKED");
    }

    @Test
    void executeQuery_returnsQueryResponse() throws SQLException {
        String sql = "SELECT 1 AS val";

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(sql)).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
        when(resultSetMetaData.getColumnCount()).thenReturn(1);
        when(resultSetMetaData.getColumnLabel(1)).thenReturn("val");
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getObject(1)).thenReturn(1);

        QueryResponse response = snowflakeService.executeQuery(sql);

        assertThat(response.sql()).isEqualTo(sql);
        assertThat(response.columns()).containsExactly("val");
        assertThat(response.rowCount()).isEqualTo(1);
        assertThat(response.rows().get(0).get("val")).isEqualTo(1);
    }

    @Test
    void executeQuery_returnsEmptyResult() throws SQLException {
        String sql = "SELECT 1 AS val WHERE 1=0";

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(sql)).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
        when(resultSetMetaData.getColumnCount()).thenReturn(1);
        when(resultSetMetaData.getColumnLabel(1)).thenReturn("val");
        when(resultSet.next()).thenReturn(false);

        QueryResponse response = snowflakeService.executeQuery(sql);

        assertThat(response.rowCount()).isZero();
        assertThat(response.rows()).isEmpty();
    }

    @Test
    void executeQuery_throwsRuntimeException_onSqlError() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenThrow(new SQLException("table not found"));

        assertThatThrownBy(() -> snowflakeService.executeQuery("SELECT * FROM bad_table"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("table not found");
    }

    private EprivacyConsent buildConsent(String datasubjectId, String purposeId,
                                         String vin, String purposeName, String status) {
        EprivacyConsent consent = new EprivacyConsent();
        consent.setDatasubjectId(datasubjectId);
        consent.setPurposeId(purposeId);
        consent.setVin(vin);
        consent.setPurposeName(purposeName);
        consent.setPurposeStatus(status);
        consent.setPurposeDate(LocalDateTime.of(2025, 6, 15, 10, 30));
        consent.setPurposeVersion("1.0");
        consent.setBrand("BMW");
        consent.setCountryCode("DE");
        consent.setDeleteFlag("N");
        consent.setCreatedDate(LocalDateTime.of(2025, 1, 1, 0, 0));
        consent.setUpdatedDate(LocalDateTime.of(2025, 6, 15, 10, 30));
        return consent;
    }
}
