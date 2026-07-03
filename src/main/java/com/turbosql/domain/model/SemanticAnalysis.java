package com.turbosql.domain.model;

import java.util.List;

public record SemanticAnalysis(
    boolean valid, List<SemanticIssue> errors, List<SemanticIssue> warnings) {

  public SemanticAnalysis {
    errors = errors == null ? List.of() : List.copyOf(errors);
    warnings = warnings == null ? List.of() : List.copyOf(warnings);
  }
}
