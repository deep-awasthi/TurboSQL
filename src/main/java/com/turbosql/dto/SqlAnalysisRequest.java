package com.turbosql.dto;

import jakarta.validation.constraints.NotBlank;

public record SqlAnalysisRequest(
    @NotBlank(message = "dialect is required") String dialect,
    @NotBlank(message = "sql is required") String sql) {}
