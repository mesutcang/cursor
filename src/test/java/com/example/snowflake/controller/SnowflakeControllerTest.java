package com.example.snowflake.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.example.snowflake.model.EprivacyConsent;
import com.example.snowflake.model.EprivacyConsentResponse;
import com.example.snowflake.model.QueryResponse;
import com.example.snowflake.service.SnowflakeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SnowflakeController.class)
class SnowflakeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SnowflakeService snowflakeService;

    @Test
    void getEprivacyConsentByVin_returns200_whenRecordsFound() throws Exception {
        String vin = "WBA12345678901234";
        EprivacyConsent consent = buildConsent("DS-001", "PUR-001", vin, "Marketing", "ACTIVE");
        EprivacyConsentResponse response = new EprivacyConsentResponse(vin, List.of(consent), 1);

        when(snowflakeService.findEprivacyConsentByVin(vin)).thenReturn(response);

        mockMvc.perform(get("/api/snowflake/eprivacy-consent/{vin}", vin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vin", is(vin)))
                .andExpect(jsonPath("$.totalRecords", is(1)))
                .andExpect(jsonPath("$.consents", hasSize(1)))
                .andExpect(jsonPath("$.consents[0].vin", is(vin)))
                .andExpect(jsonPath("$.consents[0].purposeName", is("Marketing")))
                .andExpect(jsonPath("$.consents[0].purposeStatus", is("ACTIVE")))
                .andExpect(jsonPath("$.consents[0].brand", is("BMW")));
    }

    @Test
    void getEprivacyConsentByVin_returns404_whenNoRecords() throws Exception {
        String vin = "NONEXISTENT";
        EprivacyConsentResponse response = new EprivacyConsentResponse(vin, List.of(), 0);

        when(snowflakeService.findEprivacyConsentByVin(vin)).thenReturn(response);

        mockMvc.perform(get("/api/snowflake/eprivacy-consent/{vin}", vin))
                .andExpect(status().isNotFound());
    }

    @Test
    void getEprivacyConsentByVin_returnsMultipleConsents() throws Exception {
        String vin = "WBA12345678901234";
        EprivacyConsent c1 = buildConsent("DS-001", "PUR-001", vin, "Marketing", "ACTIVE");
        EprivacyConsent c2 = buildConsent("DS-001", "PUR-002", vin, "Telemetry", "REVOKED");
        EprivacyConsentResponse response = new EprivacyConsentResponse(vin, List.of(c1, c2), 2);

        when(snowflakeService.findEprivacyConsentByVin(vin)).thenReturn(response);

        mockMvc.perform(get("/api/snowflake/eprivacy-consent/{vin}", vin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords", is(2)))
                .andExpect(jsonPath("$.consents", hasSize(2)))
                .andExpect(jsonPath("$.consents[0].purposeName", is("Marketing")))
                .andExpect(jsonPath("$.consents[1].purposeName", is("Telemetry")));
    }

    @Test
    void getVersion_returns200() throws Exception {
        QueryResponse response = new QueryResponse(
                "SELECT CURRENT_VERSION() AS snowflake_version",
                List.of("snowflake_version"),
                List.of(Map.of("snowflake_version", "8.30.1")),
                1);

        when(snowflakeService.executeQuery(anyString())).thenReturn(response);

        mockMvc.perform(get("/api/snowflake/version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rowCount", is(1)))
                .andExpect(jsonPath("$.rows[0].snowflake_version", is("8.30.1")));
    }

    @Test
    void getCurrentSession_returns200() throws Exception {
        QueryResponse response = new QueryResponse(
                "SELECT CURRENT_USER()",
                List.of("current_user", "current_role"),
                List.of(Map.of("current_user", "TEST_USER", "current_role", "SYSADMIN")),
                1);

        when(snowflakeService.executeQuery(anyString())).thenReturn(response);

        mockMvc.perform(get("/api/snowflake/current-session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rowCount", is(1)))
                .andExpect(jsonPath("$.rows[0].current_user", is("TEST_USER")));
    }

    @Test
    void executeQuery_returns200() throws Exception {
        QueryResponse response = new QueryResponse(
                "SELECT 1",
                List.of("1"),
                List.of(Map.of("1", 1)),
                1);

        when(snowflakeService.executeQuery(anyString())).thenReturn(response);

        mockMvc.perform(post("/api/snowflake/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\": \"SELECT 1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rowCount", is(1)));
    }

    @Test
    void listTables_returns200() throws Exception {
        QueryResponse response = new QueryResponse(
                "SHOW TABLES IN SCHEMA MY_DB.PUBLIC",
                List.of("name"),
                List.of(Map.of("name", "EPRIVACY_CONSENT")),
                1);

        when(snowflakeService.executeQuery(anyString())).thenReturn(response);

        mockMvc.perform(get("/api/snowflake/tables")
                        .param("database", "MY_DB")
                        .param("schema", "PUBLIC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rowCount", is(1)));
    }

    @Test
    void listTables_returns400_whenDatabaseMissing() throws Exception {
        mockMvc.perform(get("/api/snowflake/tables"))
                .andExpect(status().isBadRequest());
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
