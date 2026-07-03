package com.turbosql.domain.service;

import com.turbosql.domain.model.OptimizationRuleResult;
import com.turbosql.domain.model.Token;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RuleBasedOptimizer {

  public List<OptimizationRuleResult> optimize(List<Token> tokens) {
    String sql = SqlTokenUtils.join(tokens);
    List<OptimizationRuleResult> rules = new ArrayList<>();
    add(rules, "Predicate Pushdown", SqlTokenUtils.containsKeyword(tokens, "WHERE"), sql, "Filters are moved closer to scans.");
    add(rules, "Projection Pushdown", !sql.contains("*"), sql, "Only referenced columns are projected from lower operators.");
    add(rules, "Constant Folding", containsNumericExpression(tokens), sql, "Constant expressions can be reduced before planning.");
    add(rules, "Boolean Simplification", containsBooleanSimplification(tokens), sql, "Boolean predicates are normalized.");
    add(rules, "Join Reordering", SqlTokenUtils.countKeyword(tokens, "JOIN") > 1, sql, "Multiple joins can be ordered by estimated cost.");
    add(rules, "Join Elimination", false, sql, "No removable join was detected.");
    add(rules, "Dead Predicate Removal", containsContradiction(tokens), sql, "Contradictory predicates can short-circuit the plan.");
    add(rules, "Subquery Flattening", SqlTokenUtils.countKeyword(tokens, "SELECT") > 1, sql, "Simple subqueries can be flattened.");
    add(rules, "Filter Merge", repeatedKeyword(tokens, "WHERE") || SqlTokenUtils.countKeyword(tokens, "AND") > 1, sql, "Adjacent filters are merged.");
    add(rules, "Expression Simplification", containsNumericExpression(tokens), sql, "Expressions are simplified.");
    add(rules, "Distinct Elimination", SqlTokenUtils.containsKeyword(tokens, "DISTINCT") && SqlTokenUtils.containsKeyword(tokens, "GROUP"), sql, "DISTINCT is redundant for grouped keys.");
    add(rules, "Sort Elimination", SqlTokenUtils.containsKeyword(tokens, "ORDER") && SqlTokenUtils.containsKeyword(tokens, "LIMIT"), sql, "Sort can be bounded by LIMIT.");
    add(rules, "Limit Pushdown", SqlTokenUtils.containsKeyword(tokens, "LIMIT"), sql, "LIMIT can reduce upstream row counts.");
    add(rules, "Aggregation Pushdown", SqlTokenUtils.containsKeyword(tokens, "GROUP"), sql, "Aggregations can be moved below joins when keys allow it.");
    add(rules, "Window Optimization", SqlTokenUtils.containsKeyword(tokens, "OVER"), sql, "Window partition/order operations are consolidated.");
    add(rules, "Common Subexpression Elimination", duplicatePredicate(tokens), sql, "Repeated expressions can be evaluated once.");
    add(rules, "Predicate Simplification", SqlTokenUtils.containsKeyword(tokens, "BETWEEN") || SqlTokenUtils.containsKeyword(tokens, "IN"), sql, "Predicates are converted into canonical form.");
    return rules;
  }

  private void add(List<OptimizationRuleResult> rules, String name, boolean applied, String sql, String reason) {
    rules.add(new OptimizationRuleResult(name, applied, applied ? sql : null, applied ? sql : null, reason));
  }

  private boolean containsNumericExpression(List<Token> tokens) {
    for (int i = 1; i < tokens.size() - 1; i++) {
      if (tokens.get(i).value().matches("[+\\-*/]")
          && tokens.get(i - 1).type().name().equals("LITERAL")
          && tokens.get(i + 1).type().name().equals("LITERAL")) {
        return true;
      }
    }
    return false;
  }

  private boolean containsBooleanSimplification(List<Token> tokens) {
    String sql = SqlTokenUtils.join(tokens).toUpperCase();
    return sql.contains("1 = 1") || sql.contains("TRUE AND") || sql.contains("FALSE OR");
  }

  private boolean containsContradiction(List<Token> tokens) {
    String sql = SqlTokenUtils.join(tokens).toUpperCase();
    return sql.contains("1 = 0") || sql.contains("FALSE AND");
  }

  private boolean repeatedKeyword(List<Token> tokens, String keyword) {
    return SqlTokenUtils.countKeyword(tokens, keyword) > 1;
  }

  private boolean duplicatePredicate(List<Token> tokens) {
    String where = SqlTokenUtils.join(SqlTokenUtils.between(tokens, "WHERE", List.of("GROUP", "HAVING", "ORDER", "LIMIT", "OFFSET", "FETCH"))).toUpperCase();
    if (where.isBlank()) {
      return false;
    }
    List<String> predicates = List.of(where.split(" AND "));
    return predicates.size() != predicates.stream().distinct().count();
  }
}
