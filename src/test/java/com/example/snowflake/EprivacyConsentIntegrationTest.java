package com.example.snowflake;

import javax.sql.DataSource;

import com.example.snowflake.config.SnowflakeConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(EprivacyConsentIntegrationTest.H2DataSourceConfig.class)
class EprivacyConsentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class H2DataSourceConfig {

        @Bean
        @Primary
        public DataSource dataSource() {
            return DataSourceBuilder.create()
                    .url("jdbc:h2:mem:testdb;MODE=LEGACY")
                    .driverClassName("org.h2.Driver")
                    .username("sa")
                    .password("")
                    .build();
        }
    }

    @Test
    void getEprivacyConsentByVin_returns200WithConsents_whenVinExists() throws Exception {
        mockMvc.perform(get("/api/snowflake/eprivacy-consent/WBA12345678901234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vin", is("WBA12345678901234")))
                .andExpect(jsonPath("$.totalRecords", is(2)))
                .andExpect(jsonPath("$.consents", hasSize(2)))
                .andExpect(jsonPath("$.consents[0].datasubjectId", is("DS-001")))
                .andExpect(jsonPath("$.consents[0].brand", is("BMW")))
                .andExpect(jsonPath("$.consents[0].countryCode", is("DE")));
    }

    @Test
    void getEprivacyConsentByVin_returns404_whenVinDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/snowflake/eprivacy-consent/NONEXISTENT_VIN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getEprivacyConsentByVin_returnsSingleConsent_whenVinHasOneRecord() throws Exception {
        mockMvc.perform(get("/api/snowflake/eprivacy-consent/WBA98765432109876"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vin", is("WBA98765432109876")))
                .andExpect(jsonPath("$.totalRecords", is(1)))
                .andExpect(jsonPath("$.consents", hasSize(1)))
                .andExpect(jsonPath("$.consents[0].purposeName", is("Marketing Consent")))
                .andExpect(jsonPath("$.consents[0].purposeStatus", is("ACTIVE")))
                .andExpect(jsonPath("$.consents[0].brand", is("TOYOTA")))
                .andExpect(jsonPath("$.consents[0].countryCode", is("FR")))
                .andExpect(jsonPath("$.consents[0].language", is("fr")));
    }

    @Test
    void getEprivacyConsentByVin_verifiesAllFieldsMapped() throws Exception {
        mockMvc.perform(get("/api/snowflake/eprivacy-consent/WBA98765432109876"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consents[0].datasubjectId", is("DS-002")))
                .andExpect(jsonPath("$.consents[0].purposeId", is("PUR-001")))
                .andExpect(jsonPath("$.consents[0].vin", is("WBA98765432109876")))
                .andExpect(jsonPath("$.consents[0].purposeName", is("Marketing Consent")))
                .andExpect(jsonPath("$.consents[0].purposeStatus", is("ACTIVE")))
                .andExpect(jsonPath("$.consents[0].purposeVersion", is("1.0")))
                .andExpect(jsonPath("$.consents[0].purposeGlobalCode", is("GC-MKT-001")))
                .andExpect(jsonPath("$.consents[0].panePurposeName", is("Marketing Pane")))
                .andExpect(jsonPath("$.consents[0].unifiedCustomerId", is("UC-002")))
                .andExpect(jsonPath("$.consents[0].portalUuid", is("portal-uuid-002")))
                .andExpect(jsonPath("$.consents[0].brand", is("TOYOTA")))
                .andExpect(jsonPath("$.consents[0].nmscCode", is("NMSC-FR")))
                .andExpect(jsonPath("$.consents[0].countryCode", is("FR")))
                .andExpect(jsonPath("$.consents[0].sourceSystemName", is("PORTAL")))
                .andExpect(jsonPath("$.consents[0].language", is("fr")))
                .andExpect(jsonPath("$.consents[0].deleteFlag", is("N")))
                .andExpect(jsonPath("$.consents[0].deleteDate").doesNotExist());
    }
}
