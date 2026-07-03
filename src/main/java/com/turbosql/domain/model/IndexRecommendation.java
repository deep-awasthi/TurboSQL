package com.turbosql.domain.model;

import java.util.List;

public record IndexRecommendation(
    String type, String table, List<String> recommendedColumns, String reason, double estimatedBenefit) {

  public IndexRecommendation {
    recommendedColumns = recommendedColumns == null ? List.of() : List.copyOf(recommendedColumns);
  }
}
