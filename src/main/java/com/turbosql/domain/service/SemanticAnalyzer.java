package com.turbosql.domain.service;

import com.turbosql.domain.model.AstNode;
import com.turbosql.domain.model.SemanticAnalysis;
import com.turbosql.domain.model.SemanticIssue;
import com.turbosql.domain.model.Token;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SemanticAnalyzer {

  private static final Set<String> AGGREGATES = Set.of("COUNT", "SUM", "AVG", "MIN", "MAX");

  public SemanticAnalysis analyze(AstNode ast, List<Token> tokens) {
    List<SemanticIssue> errors = new ArrayList<>();
    List<SemanticIssue> warnings = new ArrayList<>();
    detectDuplicateAliases(tokens, errors);
    detectHavingWithoutAggregation(tokens, errors);
    detectAggregateInWhere(tokens, errors);
    detectAmbiguousColumns(tokens, warnings);
    detectGroupByIssues(tokens, errors);
    return new SemanticAnalysis(errors.isEmpty(), errors, warnings);
  }

  private void detectDuplicateAliases(List<Token> tokens, List<SemanticIssue> errors) {
    Set<String> aliases = new HashSet<>();
    List<TableRef> refs = extractTableRefs(tokens);
    for (TableRef ref : refs) {
      String alias = ref.alias().toUpperCase();
      if (!aliases.add(alias)) {
        errors.add(new SemanticIssue("HIGH", "DUPLICATE_ALIAS", "Duplicate table alias " + ref.alias(), ref.line(), ref.column()));
      }
    }
  }

  private void detectHavingWithoutAggregation(List<Token> tokens, List<SemanticIssue> errors) {
    if (SqlTokenUtils.containsKeyword(tokens, "HAVING")
        && !SqlTokenUtils.containsKeyword(tokens, "GROUP")
        && tokens.stream().noneMatch(token -> AGGREGATES.contains(token.value().toUpperCase()))) {
      Token having = first(tokens, "HAVING");
      errors.add(new SemanticIssue("MEDIUM", "INVALID_HAVING", "HAVING requires GROUP BY or aggregate expressions", having.line(), having.column()));
    }
  }

  private void detectAggregateInWhere(List<Token> tokens, List<SemanticIssue> errors) {
    List<Token> where = SqlTokenUtils.between(tokens, "WHERE", List.of("GROUP", "HAVING", "ORDER", "LIMIT", "OFFSET", "FETCH"));
    for (Token token : where) {
      if (AGGREGATES.contains(token.value().toUpperCase())) {
        errors.add(new SemanticIssue("HIGH", "AGGREGATE_MISUSE", "Aggregate function cannot be used in WHERE", token.line(), token.column()));
      }
    }
  }

  private void detectAmbiguousColumns(List<Token> tokens, List<SemanticIssue> warnings) {
    if (extractTableRefs(tokens).size() < 2) {
      return;
    }
    List<Token> projection = SqlTokenUtils.between(tokens, "SELECT", List.of("FROM"));
    for (Token token : projection) {
      if (SqlTokenUtils.isIdentifier(token) && !isQualified(tokens, token)) {
        warnings.add(new SemanticIssue("LOW", "POSSIBLY_AMBIGUOUS_COLUMN", "Unqualified column " + token.value() + " appears with multiple tables", token.line(), token.column()));
      }
    }
  }

  private void detectGroupByIssues(List<Token> tokens, List<SemanticIssue> errors) {
    if (!SqlTokenUtils.containsKeyword(tokens, "GROUP")) {
      return;
    }
    List<Token> projection = SqlTokenUtils.between(tokens, "SELECT", List.of("FROM"));
    String groupBy = SqlTokenUtils.join(SqlTokenUtils.between(tokens, "GROUP", List.of("HAVING", "ORDER", "LIMIT", "OFFSET", "FETCH"))).toUpperCase();
    for (List<Token> item : SqlTokenUtils.splitTopLevel(projection, ",")) {
      if (item.isEmpty() || containsAggregate(item) || item.stream().anyMatch(token -> token.value().equals("*"))) {
        continue;
      }
      String selected = SqlTokenUtils.join(stripAlias(item)).toUpperCase();
      if (!selected.isBlank() && !groupBy.contains(selected)) {
        Token token = item.get(0);
        errors.add(new SemanticIssue("HIGH", "INVALID_GROUP_BY", "Selected expression is not grouped or aggregated: " + selected, token.line(), token.column()));
      }
    }
  }

  private boolean containsAggregate(List<Token> tokens) {
    return tokens.stream().anyMatch(token -> AGGREGATES.contains(token.value().toUpperCase()));
  }

  private List<Token> stripAlias(List<Token> tokens) {
    List<Token> result = new ArrayList<>();
    for (int i = 0; i < tokens.size(); i++) {
      if (tokens.get(i).value().equalsIgnoreCase("AS")) {
        break;
      }
      result.add(tokens.get(i));
    }
    return result;
  }

  private boolean isQualified(List<Token> tokens, Token candidate) {
    for (int i = 1; i < tokens.size(); i++) {
      if (tokens.get(i) == candidate && tokens.get(i - 1).value().equals(".")) {
        return true;
      }
    }
    return false;
  }

  private Token first(List<Token> tokens, String keyword) {
    return tokens.stream().filter(token -> token.value().equalsIgnoreCase(keyword)).findFirst().orElse(tokens.get(0));
  }

  private List<TableRef> extractTableRefs(List<Token> tokens) {
    List<TableRef> refs = new ArrayList<>();
    boolean expectTable = false;
    for (int i = 0; i < tokens.size(); i++) {
      String value = tokens.get(i).value().toUpperCase();
      if (value.equals("FROM") || value.equals("JOIN") || value.equals(",")) {
        expectTable = true;
        continue;
      }
      if (expectTable && SqlTokenUtils.isIdentifier(tokens.get(i))) {
        String table = tokens.get(i).value();
        String alias = table;
        int aliasIndex = i + 1;
        if (aliasIndex < tokens.size() && tokens.get(aliasIndex).value().equalsIgnoreCase("AS")) {
          aliasIndex++;
        }
        if (aliasIndex < tokens.size()
            && SqlTokenUtils.isIdentifier(tokens.get(aliasIndex))
            && !tokens.get(aliasIndex).value().equalsIgnoreCase("ON")) {
          alias = tokens.get(aliasIndex).value();
        }
        refs.add(new TableRef(table, alias, tokens.get(i).line(), tokens.get(i).column()));
        expectTable = false;
      }
    }
    return refs;
  }

  private record TableRef(String table, String alias, int line, int column) {}
}
