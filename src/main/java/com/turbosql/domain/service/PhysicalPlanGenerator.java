package com.turbosql.domain.service;

import com.turbosql.domain.model.TreeNode;
import com.turbosql.domain.model.Token;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PhysicalPlanGenerator {

  public TreeNode generate(List<Token> tokens) {
    List<TreeNode> operators = new ArrayList<>();
    String from = SqlTokenUtils.join(SqlTokenUtils.between(tokens, "FROM", List.of("WHERE", "GROUP", "HAVING", "ORDER", "LIMIT", "OFFSET", "FETCH")));
    String where = SqlTokenUtils.join(SqlTokenUtils.between(tokens, "WHERE", List.of("GROUP", "HAVING", "ORDER", "LIMIT", "OFFSET", "FETCH")));
    if (from.isBlank()) {
      operators.add(TreeNode.of("Constant", "single-row", List.of()));
    } else if (where.contains("=")) {
      operators.add(TreeNode.of("Index Scan", from, List.of()));
    } else {
      operators.add(TreeNode.of("Sequential Scan", from, List.of()));
    }
    if (SqlTokenUtils.containsKeyword(tokens, "JOIN")) {
      String type = SqlTokenUtils.containsKeyword(tokens, "ORDER") ? "Merge Join" : "Hash Join";
      operators.add(TreeNode.of(type, "join predicates", List.of()));
    }
    if (SqlTokenUtils.containsKeyword(tokens, "GROUP")) {
      operators.add(TreeNode.of("Hash Aggregate", "grouped rows", List.of()));
    }
    if (SqlTokenUtils.containsKeyword(tokens, "ORDER")) {
      operators.add(TreeNode.of("Sort", "order keys", List.of()));
    }
    if (SqlTokenUtils.containsKeyword(tokens, "LIMIT") || SqlTokenUtils.containsKeyword(tokens, "FETCH")) {
      operators.add(TreeNode.of("Limit", "bounded result", List.of()));
    }
    return new TreeNode("PhysicalPlan", "pipeline", operators, Map.of("operators", operators.size()));
  }
}
