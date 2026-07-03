package com.turbosql.domain.service;

import com.turbosql.common.exception.SqlSyntaxException;
import com.turbosql.domain.model.AstNode;
import com.turbosql.domain.model.Token;
import com.turbosql.domain.model.TokenType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class SqlParser {

  private static final List<String> CLAUSES =
      List.of("FROM", "WHERE", "GROUP", "HAVING", "ORDER", "LIMIT", "OFFSET", "FETCH");
  private static final List<String> SET_OPERATORS = List.of("UNION", "INTERSECT", "EXCEPT");
  private final AtomicInteger nodeIds = new AtomicInteger();

  public AstNode parse(List<Token> input) {
    nodeIds.set(0);
    List<Token> tokens = withoutEof(input);
    if (tokens.isEmpty()) {
      throw new SqlSyntaxException("SQL statement is required", 1, 1);
    }
    validateParentheses(tokens);
    ParserCursor cursor = new ParserCursor(tokens);
    if (cursor.match("WITH") || cursor.check("SELECT")) {
      return parseSelect(cursor);
    }
    String first = cursor.peek().value().toUpperCase();
    if (List.of("CREATE", "ALTER", "DROP", "TRUNCATE", "COMMENT", "RENAME").contains(first)) {
      return parseGeneric(cursor, "DDL_" + first);
    }
    if (List.of("INSERT", "UPDATE", "DELETE", "MERGE", "UPSERT").contains(first)) {
      return parseGeneric(cursor, "DML_" + first);
    }
    if (List.of("BEGIN", "COMMIT", "ROLLBACK", "SAVEPOINT").contains(first)) {
      return parseGeneric(cursor, "TRANSACTION_" + first);
    }
    throw new SqlSyntaxException("Unsupported statement " + cursor.peek().value(), cursor.peek().line(), cursor.peek().column());
  }

  private AstNode parseSelect(ParserCursor cursor) {
    List<AstNode> children = new ArrayList<>();
    if (cursor.previousMatched("WITH")) {
      children.add(parseClause(cursor, "CTE", List.of("SELECT")));
    }
    Token select = cursor.consume("SELECT", "Expected SELECT");
    List<Token> projection = cursor.consumeUntil(List.of("FROM", "WHERE", "GROUP", "HAVING", "ORDER", "LIMIT", "OFFSET", "FETCH", "UNION", "INTERSECT", "EXCEPT"));
    if (projection.isEmpty()) {
      Token found = cursor.isAtEnd() ? select : cursor.peek();
      throw new SqlSyntaxException("Unexpected token " + found.value(), found.line(), found.column());
    }
    children.add(sequenceNode("PROJECTION", projection, projectionMetadata(projection)));
    if (cursor.match("FROM")) {
      List<Token> from = cursor.consumeUntil(List.of("WHERE", "GROUP", "HAVING", "ORDER", "LIMIT", "OFFSET", "FETCH", "UNION", "INTERSECT", "EXCEPT"));
      if (from.isEmpty()) {
        Token found = cursor.isAtEnd() ? select : cursor.peek();
        throw new SqlSyntaxException("Expected table reference after FROM", found.line(), found.column());
      }
      children.add(sequenceNode("FROM", from, Map.of("tables", extractTables(from))));
    }
    if (cursor.match("WHERE")) {
      children.add(parseClause(cursor, "WHERE", List.of("GROUP", "HAVING", "ORDER", "LIMIT", "OFFSET", "FETCH", "UNION", "INTERSECT", "EXCEPT")));
    }
    if (cursor.match("GROUP")) {
      cursor.consume("BY", "Expected BY after GROUP");
      children.add(parseClause(cursor, "GROUP_BY", List.of("HAVING", "ORDER", "LIMIT", "OFFSET", "FETCH", "UNION", "INTERSECT", "EXCEPT")));
    }
    if (cursor.match("HAVING")) {
      children.add(parseClause(cursor, "HAVING", List.of("ORDER", "LIMIT", "OFFSET", "FETCH", "UNION", "INTERSECT", "EXCEPT")));
    }
    if (cursor.match("ORDER")) {
      cursor.consume("BY", "Expected BY after ORDER");
      children.add(parseClause(cursor, "ORDER_BY", List.of("LIMIT", "OFFSET", "FETCH", "UNION", "INTERSECT", "EXCEPT")));
    }
    if (cursor.match("LIMIT")) {
      children.add(parseClause(cursor, "LIMIT", List.of("OFFSET", "FETCH", "UNION", "INTERSECT", "EXCEPT")));
    }
    if (cursor.match("OFFSET")) {
      children.add(parseClause(cursor, "OFFSET", List.of("FETCH", "UNION", "INTERSECT", "EXCEPT")));
    }
    if (cursor.match("FETCH")) {
      children.add(parseClause(cursor, "FETCH", SET_OPERATORS));
    }
    if (!cursor.isAtEnd() && SET_OPERATORS.contains(cursor.peek().value().toUpperCase())) {
      children.add(parseClause(cursor, "SET_OPERATION", List.of()));
    }
    return node("STATEMENT_SELECT", "SELECT", children, Map.of("statementType", "SELECT"));
  }

  private AstNode parseGeneric(ParserCursor cursor, String type) {
    List<Token> tokens = cursor.consumeUntil(List.of());
    return sequenceNode(type, tokens, Map.of("statementType", type));
  }

  private AstNode parseClause(ParserCursor cursor, String type, List<String> terminators) {
    List<Token> tokens = cursor.consumeUntil(terminators);
    return sequenceNode(type, tokens, Map.of());
  }

  private AstNode sequenceNode(String type, List<Token> tokens, Map<String, Object> metadata) {
    List<AstNode> children = tokens.stream()
        .map(token -> AstNode.leaf(nextId(), token.type().name(), token.value()))
        .toList();
    return node(type, join(tokens), children, metadata);
  }

  private AstNode node(String type, String value, List<AstNode> children, Map<String, Object> metadata) {
    return new AstNode(nextId(), type, value, children, metadata);
  }

  private Map<String, Object> projectionMetadata(List<Token> tokens) {
    return Map.of("items", splitTopLevel(tokens).stream().map(this::join).toList());
  }

  private List<Map<String, String>> extractTables(List<Token> tokens) {
    List<Map<String, String>> tables = new ArrayList<>();
    boolean expectTable = true;
    for (int index = 0; index < tokens.size(); index++) {
      Token token = tokens.get(index);
      String value = token.value().toUpperCase();
      if (expectTable && isName(token)) {
        String table = token.value();
        String alias = null;
        int aliasIndex = index + 1;
        if (aliasIndex < tokens.size() && tokens.get(aliasIndex).value().equalsIgnoreCase("AS")) {
          aliasIndex++;
        }
        if (aliasIndex < tokens.size() && isName(tokens.get(aliasIndex))) {
          String candidate = tokens.get(aliasIndex).value();
          if (!List.of("ON", "USING", "JOIN", "WHERE", "INNER", "LEFT", "RIGHT", "FULL", "CROSS", "NATURAL", "LATERAL").contains(candidate.toUpperCase())) {
            alias = candidate;
          }
        }
        Map<String, String> tableEntry = new LinkedHashMap<>();
        tableEntry.put("table", table);
        tableEntry.put("alias", alias == null ? table : alias);
        tables.add(tableEntry);
        expectTable = false;
      }
      if (value.equals(",") || value.equals("JOIN")) {
        expectTable = true;
      }
    }
    return tables;
  }

  private List<List<Token>> splitTopLevel(List<Token> tokens) {
    List<List<Token>> parts = new ArrayList<>();
    List<Token> current = new ArrayList<>();
    int depth = 0;
    for (Token token : tokens) {
      if (token.value().equals("(")) {
        depth++;
      } else if (token.value().equals(")")) {
        depth--;
      }
      if (token.value().equals(",") && depth == 0) {
        parts.add(List.copyOf(current));
        current.clear();
      } else {
        current.add(token);
      }
    }
    if (!current.isEmpty()) {
      parts.add(List.copyOf(current));
    }
    return parts;
  }

  private String join(List<Token> tokens) {
    StringBuilder builder = new StringBuilder();
    for (Token token : tokens) {
      if (builder.length() > 0 && needsSpace(builder.charAt(builder.length() - 1), token.value())) {
        builder.append(' ');
      }
      builder.append(token.value());
    }
    return builder.toString();
  }

  private boolean needsSpace(char previous, String current) {
    return previous != '(' && previous != '.' && !current.equals(")") && !current.equals(",") && !current.equals(".");
  }

  private void validateParentheses(List<Token> tokens) {
    int depth = 0;
    for (Token token : tokens) {
      if (token.value().equals("(")) {
        depth++;
      } else if (token.value().equals(")")) {
        depth--;
      }
      if (depth < 0) {
        throw new SqlSyntaxException("Unexpected closing parenthesis", token.line(), token.column());
      }
    }
    if (depth != 0) {
      Token last = tokens.get(tokens.size() - 1);
      throw new SqlSyntaxException("Unbalanced parentheses", last.line(), last.column());
    }
  }

  private List<Token> withoutEof(List<Token> input) {
    return input.stream().filter(token -> token.type() != TokenType.EOF).toList();
  }

  private boolean isName(Token token) {
    return token.type() == TokenType.IDENTIFIER || token.type() == TokenType.KEYWORD;
  }

  private String nextId() {
    return "ast-" + nodeIds.incrementAndGet();
  }

  private static final class ParserCursor {
    private final List<Token> tokens;
    private int position;
    private String previous;

    private ParserCursor(List<Token> tokens) {
      this.tokens = tokens;
    }

    private boolean isAtEnd() {
      return position >= tokens.size();
    }

    private Token peek() {
      return tokens.get(position);
    }

    private boolean check(String value) {
      return !isAtEnd() && peek().value().equalsIgnoreCase(value);
    }

    private boolean match(String value) {
      if (!check(value)) {
        return false;
      }
      previous = value;
      position++;
      return true;
    }

    private boolean previousMatched(String value) {
      return value.equalsIgnoreCase(previous);
    }

    private Token consume(String value, String message) {
      if (check(value)) {
        previous = value;
        return tokens.get(position++);
      }
      Token found = isAtEnd() ? tokens.get(tokens.size() - 1) : peek();
      throw new SqlSyntaxException(message, found.line(), found.column());
    }

    private List<Token> consumeUntil(List<String> terminators) {
      List<Token> consumed = new ArrayList<>();
      int depth = 0;
      while (!isAtEnd()) {
        Token token = peek();
        String value = token.value().toUpperCase();
        if (token.value().equals("(")) {
          depth++;
        } else if (token.value().equals(")")) {
          depth--;
        }
        if (depth == 0 && terminators.contains(value)) {
          break;
        }
        consumed.add(token);
        position++;
      }
      return consumed;
    }
  }
}
