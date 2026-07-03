package com.turbosql.domain.service;

import com.turbosql.domain.model.AstNode;
import com.turbosql.domain.model.TreeNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LogicalPlanGenerator {

  public TreeNode generate(AstNode ast) {
    List<TreeNode> pipeline = new ArrayList<>();
    if (!ast.type().equals("STATEMENT_SELECT")) {
      return new TreeNode("LogicalStatement", ast.type(), List.of(), Map.of());
    }
    value(ast, "FROM").ifPresent(from -> pipeline.add(new TreeNode("Scan", from, List.of(), Map.of())));
    value(ast, "FROM").filter(from -> from.toUpperCase().contains(" JOIN ")).ifPresent(from -> pipeline.add(new TreeNode("Join", from, List.of(), Map.of())));
    value(ast, "WHERE").ifPresent(where -> pipeline.add(new TreeNode("Filter", where, List.of(), Map.of())));
    value(ast, "GROUP_BY").ifPresent(group -> pipeline.add(new TreeNode("Aggregate", group, List.of(), Map.of())));
    value(ast, "ORDER_BY").ifPresent(order -> pipeline.add(new TreeNode("Sort", order, List.of(), Map.of())));
    value(ast, "LIMIT").ifPresent(limit -> pipeline.add(new TreeNode("Limit", limit, List.of(), Map.of())));
    value(ast, "PROJECTION").ifPresent(projection -> pipeline.add(new TreeNode("Projection", projection, List.of(), Map.of())));
    return new TreeNode("LogicalPlan", "pipeline", pipeline, Map.of("operators", pipeline.size()));
  }

  private java.util.Optional<String> value(AstNode ast, String type) {
    return ast.children().stream().filter(child -> child.type().equals(type)).map(AstNode::value).findFirst();
  }
}
