package com.turbosql.domain.model;

import java.util.List;
import java.util.Map;

public record TreeNode(String type, String value, List<TreeNode> children, Map<String, Object> metadata) {

  public TreeNode {
    children = children == null ? List.of() : List.copyOf(children);
    metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
  }

  public static TreeNode of(String type, String value, List<TreeNode> children) {
    return new TreeNode(type, value, children, Map.of());
  }
}
