package com.turbosql.domain.model;

import java.util.List;

public record AnalysisReport(
    boolean success,
    String dialect,
    List<Token> tokens,
    AstNode ast,
    SemanticAnalysis semanticAnalysis,
    TreeNode relationalAlgebra,
    TreeNode logicalPlan,
    List<OptimizationRuleResult> optimizationRules,
    TreeNode physicalPlan,
    EstimatedCost estimatedCost,
    List<IndexRecommendation> recommendedIndexes,
    List<AntiPattern> antiPatterns,
    String rewrittenQuery,
    Summary summary) {

  public AnalysisReport {
    tokens = tokens == null ? List.of() : List.copyOf(tokens);
    optimizationRules = optimizationRules == null ? List.of() : List.copyOf(optimizationRules);
    recommendedIndexes = recommendedIndexes == null ? List.of() : List.copyOf(recommendedIndexes);
    antiPatterns = antiPatterns == null ? List.of() : List.copyOf(antiPatterns);
  }
}
