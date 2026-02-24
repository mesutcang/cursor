package com.example.snowflake.repository;

import java.util.List;

import com.example.snowflake.model.EprivacyConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EprivacyConsentRepository extends JpaRepository<EprivacyConsent, String> {

    List<EprivacyConsent> findByVin(String vin);
}
