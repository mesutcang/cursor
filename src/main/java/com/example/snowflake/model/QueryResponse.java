package com.example.snowflake.model;

import java.util.List;
import java.util.Map;

public record QueryResponse(
        String sql,
        List<String> columns,
        List<Map<String, Object>> rows,
        int rowCount
) {}
