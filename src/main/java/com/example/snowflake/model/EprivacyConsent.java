package com.example.snowflake.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity mapped to the EPRIVACY_CONSENT table in the ACL_EPRIVACY_CONSENT schema.
 *
 * Adjust the @Column names below to match your actual Snowflake table columns.
 * Run "DESCRIBE TABLE ACL_EPRIVACY_CONSENT.EPRIVACY_CONSENT;" to see all columns.
 */
@Entity
@Table(name = "EPRIVACY_CONSENT", schema = "ACL_EPRIVACY_CONSENT")
public class EprivacyConsent {

    @Id
    @Column(name = "VIN")
    private String vin;

    public EprivacyConsent() {
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }
}
