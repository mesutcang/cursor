package com.example.snowflake.model;

import java.time.LocalDateTime;

public record EprivacyConsent(
        String vin,
        String consentId,
        String consentType,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime expiresAt
) {}
