package com.turbosql.domain.model;

import java.util.List;

public record RewriteResult(String originalQuery, String optimizedQuery, List<String> changes) {

  public RewriteResult {
    changes = changes == null ? List.of() : List.copyOf(changes);
  }
}
