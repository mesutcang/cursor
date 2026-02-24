package com.example.snowflake.model;

import java.io.Serializable;
import java.util.Objects;

public class EprivacyConsentId implements Serializable {

    private String datasubjectId;
    private String purposeId;
    private String vin;

    public EprivacyConsentId() {
    }

    public EprivacyConsentId(String datasubjectId, String purposeId, String vin) {
        this.datasubjectId = datasubjectId;
        this.purposeId = purposeId;
        this.vin = vin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EprivacyConsentId that)) return false;
        return Objects.equals(datasubjectId, that.datasubjectId)
                && Objects.equals(purposeId, that.purposeId)
                && Objects.equals(vin, that.vin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(datasubjectId, purposeId, vin);
    }
}
