package com.turbosql.domain.service;

import com.turbosql.domain.model.AstNode;
import com.turbosql.domain.model.TreeNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RelationalAlgebraGenerator {

  public TreeNode generate(AstNode ast) {
    if (!ast.type().equals("STATEMENT_SELECT")) {
      return new TreeNode("Statement", ast.type(), List.of(), Map.of("source", ast.value()));
    }
    List<TreeNode> children = new ArrayList<>();
    clause(ast, "FROM").ifPresent(from -> children.add(TreeNode.of("Scan", from.value(), List.of())));
    clause(ast, "WHERE").ifPresent(where -> children.add(TreeNode.of("Selection", where.value(), List.of())));
    clause(ast, "GROUP_BY").ifPresent(group -> children.add(TreeNode.of("Grouping", group.value(), List.of())));
    clause(ast, "ORDER_BY").ifPresent(order -> children.add(TreeNode.of("Sort", order.value(), List.of())));
    clause(ast, "SET_OPERATION").ifPresent(set -> children.add(TreeNode.of("Union", set.value(), List.of())));
    clause(ast, "PROJECTION").ifPresent(projection -> children.add(0, TreeNode.of("Projection", projection.value(), List.of())));
    return TreeNode.of("RelationalAlgebra", "root", children);
  }

  private java.util.Optional<AstNode> clause(AstNode ast, String type) {
    return ast.children().stream().filter(child -> child.type().equals(type)).findFirst();
  }
}
