package com.turbosql.domain.service;

import com.turbosql.common.exception.SqlSyntaxException;
import com.turbosql.domain.model.Token;
import com.turbosql.domain.model.TokenType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SqlLexer {

  private static final Set<String> KEYWORDS =
      Set.of(
          "ADD", "ALL", "ALTER", "AND", "ANY", "AS", "ASC", "AUTO_INCREMENT", "BEGIN",
          "BETWEEN", "BY", "CASE", "CHECK", "COALESCE", "COMMENT", "COMMIT", "CREATE",
          "CROSS", "CUBE", "CURRENT_DATE", "DEFAULT", "DELETE", "DENSE_RANK", "DESC",
          "DISTINCT", "DROP", "ELSE", "END", "EXCEPT", "EXISTS", "FETCH", "FIRST_VALUE",
          "FOREIGN", "FROM", "FULL", "GROUP", "GROUPING", "HAVING", "IDENTITY", "ILIKE",
          "IN", "INDEX", "INNER", "INSERT", "INTERSECT", "INTO", "IS", "JOIN", "KEY",
          "LAG", "LAST_VALUE", "LATERAL", "LEAD", "LEFT", "LIKE", "LIMIT", "MERGE",
          "NATURAL", "NOT", "NTILE", "NULL", "NULLIF", "OFFSET", "ON", "OR", "ORDER",
          "OUTER", "OVER", "PARTITION", "PRIMARY", "RANK", "RECURSIVE", "REFERENCES",
          "RENAME", "RETURNING", "RIGHT", "ROLLBACK", "ROLLUP", "ROW_NUMBER", "SAVEPOINT",
          "SELECT", "SET", "SETS", "TABLE", "THEN", "TO", "TRUNCATE", "UNION", "UNIQUE",
          "UPDATE", "UPSERT", "USING", "VALUES", "VIEW", "WHEN", "WHERE", "WITH");

  public List<Token> tokenize(String sql) {
    List<Token> tokens = new ArrayList<>();
    Cursor cursor = new Cursor(sql == null ? "" : sql);
    while (!cursor.isAtEnd()) {
      char current = cursor.peek();
      if (Character.isWhitespace(current)) {
        cursor.advanceWhitespace();
      } else if (current == '-' && cursor.peekNext() == '-') {
        cursor.advanceLineComment();
      } else if (current == '/' && cursor.peekNext() == '*') {
        cursor.advanceBlockComment();
      } else if (isIdentifierStart(current)) {
        tokens.add(readIdentifierOrKeyword(cursor));
      } else if (Character.isDigit(current)) {
        tokens.add(readNumber(cursor));
      } else if (current == '\'') {
        tokens.add(readString(cursor));
      } else if (current == '"' || current == '`') {
        tokens.add(readQuotedIdentifier(cursor));
      } else if (isPunctuation(current)) {
        tokens.add(cursor.token(TokenType.PUNCTUATION, String.valueOf(current)));
        cursor.advance();
      } else {
        tokens.add(readOperator(cursor));
      }
    }
    tokens.add(new Token(TokenType.EOF, "<EOF>", cursor.line, cursor.column, cursor.position));
    return tokens;
  }

  private Token readIdentifierOrKeyword(Cursor cursor) {
    int line = cursor.line;
    int column = cursor.column;
    int position = cursor.position;
    StringBuilder value = new StringBuilder();
    while (!cursor.isAtEnd() && isIdentifierPart(cursor.peek())) {
      value.append(cursor.peek());
      cursor.advance();
    }
    String raw = value.toString();
    String upper = raw.toUpperCase();
    TokenType type = KEYWORDS.contains(upper) ? TokenType.KEYWORD : TokenType.IDENTIFIER;
    return new Token(type, type == TokenType.KEYWORD ? upper : raw, line, column, position);
  }

  private Token readNumber(Cursor cursor) {
    int line = cursor.line;
    int column = cursor.column;
    int position = cursor.position;
    StringBuilder value = new StringBuilder();
    boolean decimal = false;
    while (!cursor.isAtEnd()) {
      char current = cursor.peek();
      if (Character.isDigit(current)) {
        value.append(current);
        cursor.advance();
      } else if (current == '.' && !decimal && Character.isDigit(cursor.peekNext())) {
        decimal = true;
        value.append(current);
        cursor.advance();
      } else {
        break;
      }
    }
    return new Token(TokenType.LITERAL, value.toString(), line, column, position);
  }

  private Token readString(Cursor cursor) {
    int line = cursor.line;
    int column = cursor.column;
    int position = cursor.position;
    StringBuilder value = new StringBuilder();
    value.append(cursor.peek());
    cursor.advance();
    while (!cursor.isAtEnd()) {
      char current = cursor.peek();
      value.append(current);
      cursor.advance();
      if (current == '\'') {
        if (!cursor.isAtEnd() && cursor.peek() == '\'') {
          value.append(cursor.peek());
          cursor.advance();
        } else {
          return new Token(TokenType.LITERAL, value.toString(), line, column, position);
        }
      }
    }
    throw new SqlSyntaxException("Unterminated string literal", line, column);
  }

  private Token readQuotedIdentifier(Cursor cursor) {
    int line = cursor.line;
    int column = cursor.column;
    int position = cursor.position;
    char quote = cursor.peek();
    StringBuilder value = new StringBuilder();
    value.append(quote);
    cursor.advance();
    while (!cursor.isAtEnd()) {
      char current = cursor.peek();
      value.append(current);
      cursor.advance();
      if (current == quote) {
        return new Token(TokenType.IDENTIFIER, value.toString(), line, column, position);
      }
    }
    throw new SqlSyntaxException("Unterminated quoted identifier", line, column);
  }

  private Token readOperator(Cursor cursor) {
    int line = cursor.line;
    int column = cursor.column;
    int position = cursor.position;
    char current = cursor.peek();
    char next = cursor.peekNext();
    if ((current == '<' && (next == '=' || next == '>'))
        || (current == '>' && next == '=')
        || (current == '!' && next == '=')
        || (current == ':' && next == ':')
        || (current == '|' && next == '|')) {
      cursor.advance();
      cursor.advance();
      return new Token(TokenType.OPERATOR, "" + current + next, line, column, position);
    }
    if ("=<>+-*/%".indexOf(current) >= 0) {
      cursor.advance();
      return new Token(TokenType.OPERATOR, String.valueOf(current), line, column, position);
    }
    throw new SqlSyntaxException("Unexpected character " + current, line, column);
  }

  private boolean isIdentifierStart(char value) {
    return Character.isLetter(value) || value == '_';
  }

  private boolean isIdentifierPart(char value) {
    return Character.isLetterOrDigit(value) || value == '_' || value == '$';
  }

  private boolean isPunctuation(char value) {
    return value == '(' || value == ')' || value == ',' || value == ';' || value == '.' || value == '['
        || value == ']';
  }

  private static final class Cursor {
    private final String sql;
    private int position;
    private int line = 1;
    private int column = 1;

    private Cursor(String sql) {
      this.sql = sql;
    }

    private boolean isAtEnd() {
      return position >= sql.length();
    }

    private char peek() {
      return isAtEnd() ? '\0' : sql.charAt(position);
    }

    private char peekNext() {
      return position + 1 >= sql.length() ? '\0' : sql.charAt(position + 1);
    }

    private void advance() {
      if (isAtEnd()) {
        return;
      }
      char current = sql.charAt(position++);
      if (current == '\n') {
        line++;
        column = 1;
      } else {
        column++;
      }
    }

    private void advanceWhitespace() {
      while (!isAtEnd() && Character.isWhitespace(peek())) {
        advance();
      }
    }

    private void advanceLineComment() {
      while (!isAtEnd() && peek() != '\n') {
        advance();
      }
    }

    private void advanceBlockComment() {
      int startLine = line;
      int startColumn = column;
      advance();
      advance();
      while (!isAtEnd()) {
        if (peek() == '*' && peekNext() == '/') {
          advance();
          advance();
          return;
        }
        advance();
      }
      throw new SqlSyntaxException("Unterminated block comment", startLine, startColumn);
    }

    private Token token(TokenType type, String value) {
      return new Token(type, value, line, column, position);
    }
  }
}
