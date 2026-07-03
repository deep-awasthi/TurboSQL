package com.turbosql.domain.service;

import com.turbosql.domain.model.AntiPattern;
import com.turbosql.domain.model.Token;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AntiPatternDetector {

  public List<AntiPattern> detect(List<Token> tokens) {
    List<AntiPattern> result = new ArrayList<>();
    String sql = SqlTokenUtils.join(tokens);
    if (sql.contains("*")) {
      result.add(new AntiPattern("SELECT_STAR", "MEDIUM", "Projection uses SELECT *", "*"));
    }
    if (SqlTokenUtils.containsKeyword(tokens, "CROSS") || hasCommaJoin(tokens)) {
      result.add(new AntiPattern("CARTESIAN_JOIN", "HIGH", "Query may produce a Cartesian join", "CROSS or comma join"));
    }
    if (SqlTokenUtils.containsKeyword(tokens, "FROM") && !SqlTokenUtils.containsKeyword(tokens, "WHERE")) {
      result.add(new AntiPattern("MISSING_WHERE", "MEDIUM", "Statement scans a table without a WHERE predicate", "FROM without WHERE"));
    }
    detectFunctionOnPredicateColumn(tokens, result);
    detectLeadingWildcardLike(tokens, result);
    if (SqlTokenUtils.containsKeyword(tokens, "DISTINCT") && SqlTokenUtils.containsKeyword(tokens, "GROUP")) {
      result.add(new AntiPattern("REDUNDANT_DISTINCT", "LOW", "DISTINCT can be redundant with GROUP BY", "DISTINCT with GROUP BY"));
    }
    detectLargeOffset(tokens, result);
    if (SqlTokenUtils.containsKeyword(tokens, "EXISTS") && SqlTokenUtils.countKeyword(tokens, "SELECT") > 1) {
      result.add(new AntiPattern("CORRELATED_SUBQUERY", "HIGH", "EXISTS subquery may be correlated", "EXISTS"));
    }
    if (SqlTokenUtils.containsKeyword(tokens, "OR")) {
      result.add(new AntiPattern("OR_PREVENTS_INDEX", "MEDIUM", "OR predicates can prevent simple index usage", "OR"));
    }
    if (SqlTokenUtils.countKeyword(tokens, "SELECT") > 2) {
      result.add(new AntiPattern("MULTIPLE_NESTED_SUBQUERIES", "HIGH", "Multiple nested SELECT statements detected", "SELECT count > 2"));
    }
    if (sql.toUpperCase().contains("CAST (") || sql.toUpperCase().contains("::")) {
      result.add(new AntiPattern("UNNECESSARY_CAST", "LOW", "Cast expression may prevent index usage", "CAST"));
    }
    return result;
  }

  private boolean hasCommaJoin(List<Token> tokens) {
    List<Token> from = SqlTokenUtils.between(tokens, "FROM", List.of("WHERE", "GROUP", "HAVING", "ORDER", "LIMIT", "OFFSET", "FETCH"));
    return from.stream().anyMatch(token -> token.value().equals(","));
  }

  private void detectFunctionOnPredicateColumn(List<Token> tokens, List<AntiPattern> result) {
    List<Token> where = SqlTokenUtils.between(tokens, "WHERE", List.of("GROUP", "HAVING", "ORDER", "LIMIT", "OFFSET", "FETCH"));
    for (int i = 0; i < where.size() - 1; i++) {
      if (SqlTokenUtils.isIdentifier(where.get(i)) && where.get(i + 1).value().equals("(")) {
        result.add(new AntiPattern("FUNCTION_ON_INDEXED_COLUMN", "HIGH", "Function call in predicate can prevent index usage", where.get(i).value()));
        return;
      }
    }
  }

  private void detectLeadingWildcardLike(List<Token> tokens, List<AntiPattern> result) {
    for (int i = 0; i < tokens.size() - 1; i++) {
      if ((tokens.get(i).value().equalsIgnoreCase("LIKE") || tokens.get(i).value().equalsIgnoreCase("ILIKE"))
          && tokens.get(i + 1).value().startsWith("'%")) {
        result.add(new AntiPattern("LEADING_WILDCARD_LIKE", "MEDIUM", "Leading wildcard LIKE cannot use a normal btree index efficiently", tokens.get(i + 1).value()));
      }
    }
  }

  private void detectLargeOffset(List<Token> tokens, List<AntiPattern> result) {
    for (int i = 0; i < tokens.size() - 1; i++) {
      if (tokens.get(i).value().equalsIgnoreCase("OFFSET")) {
        try {
          long offset = Long.parseLong(tokens.get(i + 1).value());
          if (offset > 1000) {
            result.add(new AntiPattern("LARGE_OFFSET", "MEDIUM", "Large OFFSET can force the database to scan and discard rows", String.valueOf(offset)));
          }
        } catch (NumberFormatException ignored) {
          return;
        }
      }
    }
  }
}
