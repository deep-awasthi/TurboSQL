package com.turbosql.domain.model;

public record EstimatedCost(
    double cpu,
    double memory,
    double diskIo,
    double networkIo,
    long estimatedRows,
    double cardinality,
    double selectivity,
    double scanCost,
    double joinCost,
    double sortCost,
    double hashCost,
    double totalCost) {}
