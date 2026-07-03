package com.turbosql.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.turbosql.domain.model.AnalysisReport;
import com.turbosql.domain.service.AntiPatternDetector;
import com.turbosql.domain.service.CostEstimator;
import com.turbosql.domain.service.IndexAdvisor;
import com.turbosql.domain.service.LogicalPlanGenerator;
import com.turbosql.domain.service.PhysicalPlanGenerator;
import com.turbosql.domain.service.QueryRewriter;
import com.turbosql.domain.service.RelationalAlgebraGenerator;
import com.turbosql.domain.service.RuleBasedOptimizer;
import com.turbosql.domain.service.SemanticAnalyzer;
import com.turbosql.domain.service.SqlLexer;
import com.turbosql.domain.service.SqlParser;
import org.junit.jupiter.api.Test;

class SqlAnalysisServiceTest {

  private final SqlAnalysisService service =
      new SqlAnalysisService(
          new SqlLexer(),
          new SqlParser(),
          new SemanticAnalyzer(),
          new RelationalAlgebraGenerator(),
          new LogicalPlanGenerator(),
          new RuleBasedOptimizer(),
          new CostEstimator(),
          new PhysicalPlanGenerator(),
          new IndexAdvisor(),
          new AntiPatternDetector(),
          new QueryRewriter());

  @Test
  void producesFullAnalysisReport() {
    AnalysisReport report =
        service.analyze("postgresql", "SELECT * FROM employee WHERE YEAR(created_at)=2025");

    assertThat(report.success()).isTrue();
    assertThat(report.tokens()).isNotEmpty();
    assertThat(report.ast().type()).isEqualTo("STATEMENT_SELECT");
    assertThat(report.recommendedIndexes()).isNotEmpty();
    assertThat(report.recommendedIndexes().getFirst().recommendedColumns()).contains("created_at");
    assertThat(report.antiPatterns()).extracting("name").contains("SELECT_STAR", "FUNCTION_ON_INDEXED_COLUMN");
    assertThat(report.rewrittenQuery()).contains("created_at >= DATE '2025-01-01'");
  }
}
