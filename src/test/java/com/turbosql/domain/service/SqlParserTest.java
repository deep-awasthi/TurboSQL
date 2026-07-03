package com.turbosql.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.turbosql.common.exception.SqlSyntaxException;
import com.turbosql.domain.model.AstNode;
import org.junit.jupiter.api.Test;

class SqlParserTest {

  private final SqlLexer lexer = new SqlLexer();
  private final SqlParser parser = new SqlParser();

  @Test
  void parsesSelectIntoClauseNodes() {
    AstNode ast =
        parser.parse(
            lexer.tokenize(
                "SELECT department_id, COUNT(*) FROM employee WHERE active = true GROUP BY department_id"));

    assertThat(ast.type()).isEqualTo("STATEMENT_SELECT");
    assertThat(ast.children()).extracting(AstNode::type).contains("PROJECTION", "FROM", "WHERE", "GROUP_BY");
  }

  @Test
  void rejectsMissingProjection() {
    assertThatThrownBy(() -> parser.parse(lexer.tokenize("SELECT FROM employee")))
        .isInstanceOf(SqlSyntaxException.class)
        .hasMessageContaining("Unexpected token FROM");
  }
}
