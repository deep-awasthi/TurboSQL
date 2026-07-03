package com.turbosql.domain.model;

public record SemanticIssue(
    String severity, String code, String message, Integer line, Integer column) {}
