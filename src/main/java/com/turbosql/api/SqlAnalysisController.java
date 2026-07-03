package com.turbosql.api;

import com.turbosql.application.SqlAnalysisService;
import com.turbosql.domain.model.AnalysisReport;
import com.turbosql.dto.SqlAnalysisRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class SqlAnalysisController {

  private final SqlAnalysisService service;

  public SqlAnalysisController(SqlAnalysisService service) {
    this.service = service;
  }

  @Operation(summary = "Run the full SQL analysis and optimization pipeline")
  @PostMapping("/analyze")
  public AnalysisReport analyze(@Valid @RequestBody SqlAnalysisRequest request) {
    return service.analyze(request.dialect(), request.sql());
  }

  @Operation(summary = "Tokenize SQL")
  @PostMapping("/tokenize")
  public AnalysisReport tokenize(@Valid @RequestBody SqlAnalysisRequest request) {
    return service.tokenize(request.dialect(), request.sql());
  }

  @Operation(summary = "Parse SQL")
  @PostMapping("/parse")
  public AnalysisReport parse(@Valid @RequestBody SqlAnalysisRequest request) {
    return service.parse(request.dialect(), request.sql());
  }

  @Operation(summary = "Generate AST")
  @PostMapping("/ast")
  public AnalysisReport ast(@Valid @RequestBody SqlAnalysisRequest request) {
    return service.ast(request.dialect(), request.sql());
  }

  @Operation(summary = "Generate relational algebra")
  @PostMapping("/relational-algebra")
  public AnalysisReport relationalAlgebra(@Valid @RequestBody SqlAnalysisRequest request) {
    return service.relationalAlgebra(request.dialect(), request.sql());
  }

  @Operation(summary = "Generate logical plan")
  @PostMapping("/logical-plan")
  public AnalysisReport logicalPlan(@Valid @RequestBody SqlAnalysisRequest request) {
    return service.logicalPlan(request.dialect(), request.sql());
  }

  @Operation(summary = "Apply rule-based optimizations")
  @PostMapping("/optimize")
  public AnalysisReport optimize(@Valid @RequestBody SqlAnalysisRequest request) {
    return service.optimize(request.dialect(), request.sql());
  }

  @Operation(summary = "Estimate query cost")
  @PostMapping("/cost")
  public AnalysisReport cost(@Valid @RequestBody SqlAnalysisRequest request) {
    return service.cost(request.dialect(), request.sql());
  }

  @Operation(summary = "Generate physical plan")
  @PostMapping("/physical-plan")
  public AnalysisReport physicalPlan(@Valid @RequestBody SqlAnalysisRequest request) {
    return service.physicalPlan(request.dialect(), request.sql());
  }

  @Operation(summary = "Recommend indexes")
  @PostMapping("/index-advisor")
  public AnalysisReport indexAdvisor(@Valid @RequestBody SqlAnalysisRequest request) {
    return service.indexAdvisor(request.dialect(), request.sql());
  }

  @Operation(summary = "Detect anti-patterns")
  @PostMapping("/anti-patterns")
  public AnalysisReport antiPatterns(@Valid @RequestBody SqlAnalysisRequest request) {
    return service.antiPatterns(request.dialect(), request.sql());
  }

  @Operation(summary = "Rewrite SQL")
  @PostMapping("/rewrite")
  public AnalysisReport rewrite(@Valid @RequestBody SqlAnalysisRequest request) {
    return service.rewrite(request.dialect(), request.sql());
  }

  @Operation(summary = "Health check")
  @PostMapping("/health")
  public ResponseEntity<Map<String, Object>> health() {
    return ResponseEntity.ok(Map.of("success", true, "status", "UP", "timestamp", Instant.now().toString()));
  }
}
