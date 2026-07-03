package com.turbosql.domain.model;

import java.util.List;
import java.util.Map;

public record AstNode(
    String id, String type, String value, List<AstNode> children, Map<String, Object> metadata) {

  public AstNode {
    children = children == null ? List.of() : List.copyOf(children);
    metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
  }

  public static AstNode leaf(String id, String type, String value) {
    return new AstNode(id, type, value, List.of(), Map.of());
  }
}
