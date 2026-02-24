package com.example.snowflake.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "EPRIVACY_CONSENT", schema = "ACL_EPRIVACY_CONSENT")
@IdClass(EprivacyConsentId.class)
public class EprivacyConsent {

    @Id
    @Column(name = "DATASUBJECT_ID")
    private String datasubjectId;

    @Id
    @Column(name = "PURPOSE_ID")
    private String purposeId;

    @Id
    @Column(name = "VIN")
    private String vin;

    @Column(name = "PURPOSE_NAME")
    private String purposeName;

    @Column(name = "PURPOSE_STATUS")
    private String purposeStatus;

    @Column(name = "PURPOSE_DATE")
    private LocalDateTime purposeDate;

    @Column(name = "PURPOSE_VERSION")
    private String purposeVersion;

    @Column(name = "PURPOSE_GLOBAL_CODE")
    private String purposeGlobalCode;

    @Column(name = "PANE_PURPOSE_NAME")
    private String panePurposeName;

    @Column(name = "UNIFIED_CUSTOMER_ID")
    private String unifiedCustomerId;

    @Column(name = "PORTAL_UUID")
    private String portalUuid;

    @Column(name = "BRAND")
    private String brand;

    @Column(name = "NMSC_CODE")
    private String nmscCode;

    @Column(name = "COUNTRY_CODE")
    private String countryCode;

    @Column(name = "SOURCE_SYSTEM_NAME")
    private String sourceSystemName;

    @Column(name = "LANGUAGE")
    private String language;

    @Column(name = "DELETE_FLAG")
    private String deleteFlag;

    @Column(name = "DELETE_DATE")
    private LocalDateTime deleteDate;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "UPDATED_DATE")
    private LocalDateTime updatedDate;

    public EprivacyConsent() {
    }

    public String getDatasubjectId() {
        return datasubjectId;
    }

    public void setDatasubjectId(String datasubjectId) {
        this.datasubjectId = datasubjectId;
    }

    public String getPurposeId() {
        return purposeId;
    }

    public void setPurposeId(String purposeId) {
        this.purposeId = purposeId;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public String getPurposeName() {
        return purposeName;
    }

    public void setPurposeName(String purposeName) {
        this.purposeName = purposeName;
    }

    public String getPurposeStatus() {
        return purposeStatus;
    }

    public void setPurposeStatus(String purposeStatus) {
        this.purposeStatus = purposeStatus;
    }

    public LocalDateTime getPurposeDate() {
        return purposeDate;
    }

    public void setPurposeDate(LocalDateTime purposeDate) {
        this.purposeDate = purposeDate;
    }

    public String getPurposeVersion() {
        return purposeVersion;
    }

    public void setPurposeVersion(String purposeVersion) {
        this.purposeVersion = purposeVersion;
    }

    public String getPurposeGlobalCode() {
        return purposeGlobalCode;
    }

    public void setPurposeGlobalCode(String purposeGlobalCode) {
        this.purposeGlobalCode = purposeGlobalCode;
    }

    public String getPanePurposeName() {
        return panePurposeName;
    }

    public void setPanePurposeName(String panePurposeName) {
        this.panePurposeName = panePurposeName;
    }

    public String getUnifiedCustomerId() {
        return unifiedCustomerId;
    }

    public void setUnifiedCustomerId(String unifiedCustomerId) {
        this.unifiedCustomerId = unifiedCustomerId;
    }

    public String getPortalUuid() {
        return portalUuid;
    }

    public void setPortalUuid(String portalUuid) {
        this.portalUuid = portalUuid;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getNmscCode() {
        return nmscCode;
    }

    public void setNmscCode(String nmscCode) {
        this.nmscCode = nmscCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getSourceSystemName() {
        return sourceSystemName;
    }

    public void setSourceSystemName(String sourceSystemName) {
        this.sourceSystemName = sourceSystemName;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getDeleteFlag() {
        return deleteFlag;
    }

    public void setDeleteFlag(String deleteFlag) {
        this.deleteFlag = deleteFlag;
    }

    public LocalDateTime getDeleteDate() {
        return deleteDate;
    }

    public void setDeleteDate(LocalDateTime deleteDate) {
        this.deleteDate = deleteDate;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }
}
