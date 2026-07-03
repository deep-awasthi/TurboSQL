package com.turbosql.dto;

public record ErrorPayload(String code, String message, Integer line, Integer column) {}
