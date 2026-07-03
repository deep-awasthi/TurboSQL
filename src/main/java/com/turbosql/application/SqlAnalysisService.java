package com.turbosql.application;

import com.turbosql.domain.model.AnalysisReport;
import com.turbosql.domain.model.AstNode;
import com.turbosql.domain.model.EstimatedCost;
import com.turbosql.domain.model.SemanticAnalysis;
import com.turbosql.domain.model.Summary;
import com.turbosql.domain.model.Token;
import com.turbosql.domain.model.TreeNode;
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
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SqlAnalysisService {

  private final SqlLexer lexer;
  private final SqlParser parser;
  private final SemanticAnalyzer semanticAnalyzer;
  private final RelationalAlgebraGenerator relationalAlgebraGenerator;
  private final LogicalPlanGenerator logicalPlanGenerator;
  private final RuleBasedOptimizer optimizer;
  private final CostEstimator costEstimator;
  private final PhysicalPlanGenerator physicalPlanGenerator;
  private final IndexAdvisor indexAdvisor;
  private final AntiPatternDetector antiPatternDetector;
  private final QueryRewriter queryRewriter;

  public SqlAnalysisService(
      SqlLexer lexer,
      SqlParser parser,
      SemanticAnalyzer semanticAnalyzer,
      RelationalAlgebraGenerator relationalAlgebraGenerator,
      LogicalPlanGenerator logicalPlanGenerator,
      RuleBasedOptimizer optimizer,
      CostEstimator costEstimator,
      PhysicalPlanGenerator physicalPlanGenerator,
      IndexAdvisor indexAdvisor,
      AntiPatternDetector antiPatternDetector,
      QueryRewriter queryRewriter) {
    this.lexer = lexer;
    this.parser = parser;
    this.semanticAnalyzer = semanticAnalyzer;
    this.relationalAlgebraGenerator = relationalAlgebraGenerator;
    this.logicalPlanGenerator = logicalPlanGenerator;
    this.optimizer = optimizer;
    this.costEstimator = costEstimator;
    this.physicalPlanGenerator = physicalPlanGenerator;
    this.indexAdvisor = indexAdvisor;
    this.antiPatternDetector = antiPatternDetector;
    this.queryRewriter = queryRewriter;
  }

  public AnalysisReport analyze(String dialect, String sql) {
    Pipeline pipeline = pipeline(sql);
    EstimatedCost cost = costEstimator.estimate(pipeline.tokens());
    return new AnalysisReport(
        true,
        dialect,
        pipeline.tokens(),
        pipeline.ast(),
        pipeline.semanticAnalysis(),
        relationalAlgebraGenerator.generate(pipeline.ast()),
        logicalPlanGenerator.generate(pipeline.ast()),
        optimizer.optimize(pipeline.tokens()),
        physicalPlanGenerator.generate(pipeline.tokens()),
        cost,
        indexAdvisor.recommend(pipeline.tokens()),
        antiPatternDetector.detect(pipeline.tokens()),
        queryRewriter.rewrite(pipeline.tokens()).optimizedQuery(),
        summary(cost, pipeline.semanticAnalysis().valid()));
  }

  public AnalysisReport tokenize(String dialect, String sql) {
    List<Token> tokens = lexer.tokenize(sql);
    return minimal(dialect, tokens, null);
  }

  public AnalysisReport parse(String dialect, String sql) {
    Pipeline pipeline = pipeline(sql);
    return minimal(dialect, pipeline.tokens(), pipeline.ast());
  }

  public AnalysisReport ast(String dialect, String sql) {
    return parse(dialect, sql);
  }

  public AnalysisReport relationalAlgebra(String dialect, String sql) {
    Pipeline pipeline = pipeline(sql);
    return new AnalysisReport(true, dialect, pipeline.tokens(), pipeline.ast(), pipeline.semanticAnalysis(), relationalAlgebraGenerator.generate(pipeline.ast()), null, null, null, null, null, null, null, null);
  }

  public AnalysisReport logicalPlan(String dialect, String sql) {
    Pipeline pipeline = pipeline(sql);
    return new AnalysisReport(true, dialect, pipeline.tokens(), pipeline.ast(), pipeline.semanticAnalysis(), null, logicalPlanGenerator.generate(pipeline.ast()), null, null, null, null, null, null, null);
  }

  public AnalysisReport optimize(String dialect, String sql) {
    Pipeline pipeline = pipeline(sql);
    return new AnalysisReport(true, dialect, pipeline.tokens(), pipeline.ast(), pipeline.semanticAnalysis(), null, null, optimizer.optimize(pipeline.tokens()), null, null, null, null, null, null);
  }

  public AnalysisReport cost(String dialect, String sql) {
    Pipeline pipeline = pipeline(sql);
    return new AnalysisReport(true, dialect, pipeline.tokens(), pipeline.ast(), pipeline.semanticAnalysis(), null, null, null, null, costEstimator.estimate(pipeline.tokens()), null, null, null, null);
  }

  public AnalysisReport physicalPlan(String dialect, String sql) {
    Pipeline pipeline = pipeline(sql);
    return new AnalysisReport(true, dialect, pipeline.tokens(), pipeline.ast(), pipeline.semanticAnalysis(), null, null, null, physicalPlanGenerator.generate(pipeline.tokens()), null, null, null, null, null);
  }

  public AnalysisReport indexAdvisor(String dialect, String sql) {
    Pipeline pipeline = pipeline(sql);
    return new AnalysisReport(true, dialect, pipeline.tokens(), pipeline.ast(), pipeline.semanticAnalysis(), null, null, null, null, null, indexAdvisor.recommend(pipeline.tokens()), null, null, null);
  }

  public AnalysisReport antiPatterns(String dialect, String sql) {
    Pipeline pipeline = pipeline(sql);
    return new AnalysisReport(true, dialect, pipeline.tokens(), pipeline.ast(), pipeline.semanticAnalysis(), null, null, null, null, null, null, antiPatternDetector.detect(pipeline.tokens()), null, null);
  }

  public AnalysisReport rewrite(String dialect, String sql) {
    Pipeline pipeline = pipeline(sql);
    return new AnalysisReport(true, dialect, pipeline.tokens(), pipeline.ast(), pipeline.semanticAnalysis(), null, null, null, null, null, null, null, queryRewriter.rewrite(pipeline.tokens()).optimizedQuery(), null);
  }

  private Pipeline pipeline(String sql) {
    List<Token> tokens = lexer.tokenize(sql);
    AstNode ast = parser.parse(tokens);
    SemanticAnalysis semanticAnalysis = semanticAnalyzer.analyze(ast, tokens);
    return new Pipeline(tokens, ast, semanticAnalysis);
  }

  private AnalysisReport minimal(String dialect, List<Token> tokens, AstNode ast) {
    return new AnalysisReport(true, dialect, tokens, ast, null, null, null, null, null, null, null, null, null, null);
  }

  private Summary summary(EstimatedCost cost, boolean semanticValid) {
    String improvement = cost.totalCost() > 100 ? "35%" : "15%";
    String risk = semanticValid ? "LOW" : "HIGH";
    return new Summary(improvement, risk);
  }

  private record Pipeline(List<Token> tokens, AstNode ast, SemanticAnalysis semanticAnalysis) {}
}
