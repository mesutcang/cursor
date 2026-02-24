package com.example.snowflake.repository;

import java.util.List;

import javax.sql.DataSource;

import com.example.snowflake.model.EprivacyConsent;
import com.example.snowflake.model.EprivacyConsentId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(EprivacyConsentRepositoryTest.H2DataSourceConfig.class)
class EprivacyConsentRepositoryTest {

    @Autowired
    private EprivacyConsentRepository repository;

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
    void findByVin_returnsMatchingRecords() {
        List<EprivacyConsent> results = repository.findByVin("WBA12345678901234");

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(c -> "WBA12345678901234".equals(c.getVin()));
    }

    @Test
    void findByVin_returnsEmptyList_whenVinNotFound() {
        List<EprivacyConsent> results = repository.findByVin("DOES_NOT_EXIST");

        assertThat(results).isEmpty();
    }

    @Test
    void findByVin_returnsSingleRecord() {
        List<EprivacyConsent> results = repository.findByVin("WBA98765432109876");

        assertThat(results).hasSize(1);
        EprivacyConsent consent = results.get(0);
        assertThat(consent.getDatasubjectId()).isEqualTo("DS-002");
        assertThat(consent.getPurposeId()).isEqualTo("PUR-001");
        assertThat(consent.getBrand()).isEqualTo("TOYOTA");
        assertThat(consent.getCountryCode()).isEqualTo("FR");
    }

    @Test
    void findById_returnsRecord_withCompositeKey() {
        EprivacyConsentId id = new EprivacyConsentId("DS-001", "PUR-001", "WBA12345678901234");

        var result = repository.findById(id);

        assertThat(result).isPresent();
        assertThat(result.get().getPurposeName()).isEqualTo("Marketing Consent");
        assertThat(result.get().getPurposeStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void findById_returnsEmpty_whenCompositeKeyNotFound() {
        EprivacyConsentId id = new EprivacyConsentId("NONE", "NONE", "NONE");

        var result = repository.findById(id);

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_returnsAllSeededRecords() {
        List<EprivacyConsent> all = repository.findAll();

        assertThat(all).hasSize(3);
    }
}
