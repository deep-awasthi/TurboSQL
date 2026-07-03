package com.turbosql.domain.model;

public record OptimizationRuleResult(
    String name, boolean applied, String before, String after, String reason) {}
