package com.example.snowflake.model;

import java.util.List;

public record EprivacyConsentResponse(
        String vin,
        List<EprivacyConsent> consents,
        int totalRecords
) {}
