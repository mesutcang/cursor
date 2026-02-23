package com.example.snowflake.model;

import java.util.List;

public record EprivacyConsentResponse(
        String vin,
        List<String> columns,
        List<EprivacyConsent> consents,
        int totalRecords
) {}
