package com.turbosql.domain.service;

import com.turbosql.domain.model.IndexRecommendation;
import com.turbosql.domain.model.Token;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class IndexAdvisor {

  public List<IndexRecommendation> recommend(List<Token> tokens) {
    List<IndexRecommendation> recommendations = new ArrayList<>();
    String table = primaryTable(tokens);
    List<String> predicateColumns = columnsBeforeOperators(tokens, List.of("=", ">", "<", ">=", "<=", "BETWEEN", "LIKE", "ILIKE"));
    if (!predicateColumns.isEmpty()) {
      recommendations.add(new IndexRecommendation("COMPOSITE", table, predicateColumns, "Columns participate in filtering predicates", benefit(predicateColumns.size())));
    }
    List<String> joinColumns = joinColumns(tokens);
    if (!joinColumns.isEmpty()) {
      recommendations.add(new IndexRecommendation("COMPOSITE", table, joinColumns, "Columns participate in join predicates", benefit(joinColumns.size()) + 5));
    }
    List<String> orderColumns = namesInClause(tokens, "ORDER", List.of("LIMIT", "OFFSET", "FETCH"));
    if (!orderColumns.isEmpty()) {
      recommendations.add(new IndexRecommendation("COVERING", table, orderColumns, "ORDER BY can benefit from index ordering", benefit(orderColumns.size())));
    }
    return recommendations;
  }

  private List<String> columnsBeforeOperators(List<Token> tokens, List<String> operators) {
    List<Token> where = SqlTokenUtils.between(tokens, "WHERE", List.of("GROUP", "HAVING", "ORDER", "LIMIT", "OFFSET", "FETCH"));
    Set<String> columns = new LinkedHashSet<>();
    for (int i = 0; i < where.size(); i++) {
      if (operators.contains(where.get(i).value().toUpperCase())) {
        String column = predicateColumn(where, i);
        if (!column.isBlank()) {
          columns.add(column);
        }
      }
    }
    return columns.stream().filter(column -> !column.isBlank()).toList();
  }

  private String predicateColumn(List<Token> where, int operatorIndex) {
    int previous = operatorIndex - 1;
    while (previous >= 0 && where.get(previous).value().equals(")")) {
      int open = matchingOpenParenthesis(where, previous);
      if (open >= 0 && open + 1 < previous && SqlTokenUtils.isIdentifier(where.get(open + 1))) {
        return cleanColumn(where.get(open + 1).value());
      }
      previous = open - 1;
    }
    while (previous >= 0 && !SqlTokenUtils.isIdentifier(where.get(previous))) {
      previous--;
    }
    return previous >= 0 ? cleanColumn(where.get(previous).value()) : "";
  }

  private int matchingOpenParenthesis(List<Token> tokens, int closeIndex) {
    int depth = 0;
    for (int i = closeIndex; i >= 0; i--) {
      if (tokens.get(i).value().equals(")")) {
        depth++;
      } else if (tokens.get(i).value().equals("(")) {
        depth--;
        if (depth == 0) {
          return i;
        }
      }
    }
    return -1;
  }

  private List<String> joinColumns(List<Token> tokens) {
    Set<String> columns = new LinkedHashSet<>();
    for (int i = 1; i < tokens.size() - 1; i++) {
      if (tokens.get(i).value().equals("=") && isQualifiedName(tokens, i - 1) && isQualifiedName(tokens, i + 1)) {
        columns.add(cleanColumn(tokens.get(i - 1).value()));
        columns.add(cleanColumn(tokens.get(i + 1).value()));
      }
    }
    return columns.stream().toList();
  }

  private List<String> namesInClause(List<Token> tokens, String start, List<String> terminators) {
    return SqlTokenUtils.between(tokens, start, terminators).stream()
        .filter(SqlTokenUtils::isIdentifier)
        .map(Token::value)
        .filter(value -> !value.equalsIgnoreCase("BY"))
        .distinct()
        .toList();
  }

  private boolean isQualifiedName(List<Token> tokens, int index) {
    return index >= 0 && index < tokens.size() && SqlTokenUtils.isIdentifier(tokens.get(index));
  }

  private String cleanColumn(String value) {
    int dot = value.lastIndexOf('.');
    return dot >= 0 ? value.substring(dot + 1) : value;
  }

  private String primaryTable(List<Token> tokens) {
    for (int i = 0; i < tokens.size() - 1; i++) {
      if (tokens.get(i).value().equalsIgnoreCase("FROM") && SqlTokenUtils.isIdentifier(tokens.get(i + 1))) {
        return tokens.get(i + 1).value();
      }
    }
    return "unknown";
  }

  private double benefit(int columns) {
    return Math.min(95.0, 20.0 + columns * 12.5);
  }
}
