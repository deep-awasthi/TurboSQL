package com.turbosql.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.turbosql.domain.model.Token;
import com.turbosql.domain.model.TokenType;
import java.util.List;
import org.junit.jupiter.api.Test;

class SqlLexerTest {

  private final SqlLexer lexer = new SqlLexer();

  @Test
  void tokenizesKeywordsIdentifiersLiteralsAndPositions() {
    List<Token> tokens = lexer.tokenize("SELECT id, 'A' FROM employee WHERE salary >= 10");

    assertThat(tokens)
        .extracting(Token::value)
        .containsExactly("SELECT", "id", ",", "'A'", "FROM", "employee", "WHERE", "salary", ">=", "10", "<EOF>");
    assertThat(tokens.get(0).type()).isEqualTo(TokenType.KEYWORD);
    assertThat(tokens.get(1).type()).isEqualTo(TokenType.IDENTIFIER);
    assertThat(tokens.get(3).type()).isEqualTo(TokenType.LITERAL);
    assertThat(tokens.get(0).line()).isEqualTo(1);
    assertThat(tokens.get(0).column()).isEqualTo(1);
  }
}
