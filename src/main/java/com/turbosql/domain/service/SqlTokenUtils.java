package com.turbosql.domain.service;

import com.turbosql.domain.model.Token;
import com.turbosql.domain.model.TokenType;
import java.util.ArrayList;
import java.util.List;

final class SqlTokenUtils {

  private SqlTokenUtils() {}

  static List<Token> withoutEof(List<Token> tokens) {
    return tokens.stream().filter(token -> token.type() != TokenType.EOF).toList();
  }

  static boolean containsKeyword(List<Token> tokens, String keyword) {
    return tokens.stream().anyMatch(token -> token.value().equalsIgnoreCase(keyword));
  }

  static long countKeyword(List<Token> tokens, String keyword) {
    return tokens.stream().filter(token -> token.value().equalsIgnoreCase(keyword)).count();
  }

  static String join(List<Token> tokens) {
    StringBuilder builder = new StringBuilder();
    for (Token token : withoutEof(tokens)) {
      if (builder.length() > 0 && needsSpace(builder.charAt(builder.length() - 1), token.value())) {
        builder.append(' ');
      }
      builder.append(token.value());
    }
    return builder.toString();
  }

  static List<List<Token>> splitTopLevel(List<Token> tokens, String delimiter) {
    List<List<Token>> parts = new ArrayList<>();
    List<Token> current = new ArrayList<>();
    int depth = 0;
    for (Token token : tokens) {
      if (token.value().equals("(")) {
        depth++;
      } else if (token.value().equals(")")) {
        depth--;
      }
      if (depth == 0 && token.value().equalsIgnoreCase(delimiter)) {
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

  static List<Token> between(List<Token> tokens, String start, List<String> terminators) {
    List<Token> clean = withoutEof(tokens);
    int startIndex = -1;
    for (int i = 0; i < clean.size(); i++) {
      if (clean.get(i).value().equalsIgnoreCase(start)) {
        startIndex = i + 1;
        if (start.equalsIgnoreCase("GROUP") || start.equalsIgnoreCase("ORDER")) {
          startIndex++;
        }
        break;
      }
    }
    if (startIndex < 0 || startIndex >= clean.size()) {
      return List.of();
    }
    List<Token> result = new ArrayList<>();
    int depth = 0;
    for (int i = startIndex; i < clean.size(); i++) {
      Token token = clean.get(i);
      if (token.value().equals("(")) {
        depth++;
      } else if (token.value().equals(")")) {
        depth--;
      }
      if (depth == 0 && terminators.contains(token.value().toUpperCase())) {
        break;
      }
      result.add(token);
    }
    return result;
  }

  static boolean isIdentifier(Token token) {
    return token.type() == TokenType.IDENTIFIER;
  }

  private static boolean needsSpace(char previous, String current) {
    return previous != '(' && previous != '.' && !current.equals(")") && !current.equals(",") && !current.equals(".");
  }
}
