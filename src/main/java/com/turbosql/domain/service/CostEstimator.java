package com.turbosql.domain.service;

import com.turbosql.domain.model.EstimatedCost;
import com.turbosql.domain.model.Token;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CostEstimator {

  public EstimatedCost estimate(List<Token> tokens) {
    long tableCount = Math.max(1, SqlTokenUtils.countKeyword(tokens, "JOIN") + (SqlTokenUtils.containsKeyword(tokens, "FROM") ? 1 : 0));
    long joinCount = SqlTokenUtils.countKeyword(tokens, "JOIN");
    boolean hasWhere = SqlTokenUtils.containsKeyword(tokens, "WHERE");
    boolean hasGroup = SqlTokenUtils.containsKeyword(tokens, "GROUP");
    boolean hasOrder = SqlTokenUtils.containsKeyword(tokens, "ORDER");
    boolean hasLimit = SqlTokenUtils.containsKeyword(tokens, "LIMIT") || SqlTokenUtils.containsKeyword(tokens, "FETCH");

    double selectivity = hasWhere ? 0.25 : 1.0;
    long estimatedRows = Math.max(1, Math.round(10000 * tableCount * selectivity / (hasLimit ? 10.0 : 1.0)));
    double scanCost = 10.0 * tableCount;
    double joinCost = joinCount == 0 ? 0 : joinCount * 25.0 * tableCount;
    double sortCost = hasOrder ? Math.log10(Math.max(estimatedRows, 10)) * 12.0 : 0;
    double hashCost = hasGroup ? Math.log10(Math.max(estimatedRows, 10)) * 8.0 : 0;
    double cpu = tokens.size() * 0.75 + joinCost + hashCost;
    double memory = (hasGroup || hasOrder ? estimatedRows * 0.003 : estimatedRows * 0.001);
    double diskIo = scanCost + (hasOrder ? 15 : 0);
    double networkIo = tableCount > 3 ? tableCount * 2.5 : 0;
    double total = cpu + memory + diskIo + networkIo + sortCost;
    return new EstimatedCost(
        round(cpu),
        round(memory),
        round(diskIo),
        round(networkIo),
        estimatedRows,
        round(estimatedRows * 1.0),
        round(selectivity),
        round(scanCost),
        round(joinCost),
        round(sortCost),
        round(hashCost),
        round(total));
  }

  private double round(double value) {
    return Math.round(value * 100.0) / 100.0;
  }
}
