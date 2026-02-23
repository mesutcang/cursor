package com.example.snowflake.controller;

import com.example.snowflake.model.QueryRequest;
import com.example.snowflake.model.QueryResponse;
import com.example.snowflake.service.SnowflakeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/snowflake")
public class SnowflakeController {

    private final SnowflakeService snowflakeService;

    public SnowflakeController(SnowflakeService snowflakeService) {
        this.snowflakeService = snowflakeService;
    }

    /**
     * POST /api/snowflake/query
     * Execute an arbitrary SQL query.
     * Body: { "sql": "SELECT ..." }
     */
    @PostMapping("/query")
    public ResponseEntity<QueryResponse> executeQuery(@RequestBody QueryRequest request) {
        QueryResponse response = snowflakeService.executeQuery(request.sql());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/snowflake/version
     * Returns the current Snowflake server version.
     */
    @GetMapping("/version")
    public ResponseEntity<QueryResponse> getVersion() {
        QueryResponse response = snowflakeService.executeQuery(
                "SELECT CURRENT_VERSION() AS snowflake_version");
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/snowflake/current-session
     * Returns metadata about the current session (user, role, database, schema, warehouse).
     */
    @GetMapping("/current-session")
    public ResponseEntity<QueryResponse> getCurrentSession() {
        QueryResponse response = snowflakeService.executeQuery(
                "SELECT CURRENT_USER() AS current_user, "
                + "CURRENT_ROLE() AS current_role, "
                + "CURRENT_DATABASE() AS current_database, "
                + "CURRENT_SCHEMA() AS current_schema, "
                + "CURRENT_WAREHOUSE() AS current_warehouse");
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/snowflake/tables?database=MY_DB&schema=PUBLIC
     * Lists tables in the specified database and schema.
     */
    @GetMapping("/tables")
    public ResponseEntity<QueryResponse> listTables(
            @RequestParam String database,
            @RequestParam(defaultValue = "PUBLIC") String schema) {
        QueryResponse response = snowflakeService.executeQuery(
                "SHOW TABLES IN SCHEMA " + database + "." + schema);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/snowflake/eprivacy-consent/{vin}
     * Looks up e-privacy consent records for the given VIN.
     * Returns 404 if no rows are found.
     */
    @GetMapping("/eprivacy-consent/{vin}")
    public ResponseEntity<QueryResponse> getEprivacyConsentByVin(@PathVariable String vin) {
        QueryResponse response = snowflakeService.executeQuery(
                "SELECT * FROM ACL_EPRIVACY_CONSENT.EPRIVACY_CONSENT WHERE VIN = ?",
                vin);

        if (response.rowCount() == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }
}
