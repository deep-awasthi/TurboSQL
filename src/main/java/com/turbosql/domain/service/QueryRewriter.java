package com.turbosql.domain.service;

import com.turbosql.domain.model.RewriteResult;
import com.turbosql.domain.model.Token;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class QueryRewriter {

  public RewriteResult rewrite(List<Token> tokens) {
    String original = SqlTokenUtils.join(tokens);
    String rewritten = original;
    List<String> changes = new ArrayList<>();
    YearPredicate yearPredicate = findYearPredicate(tokens);
    if (yearPredicate != null) {
      rewritten = original.replace(
          yearPredicate.expression(),
          yearPredicate.column()
              + " >= DATE '"
              + yearPredicate.year()
              + "-01-01' AND "
              + yearPredicate.column()
              + " < DATE '"
              + (yearPredicate.year() + 1)
              + "-01-01'");
      changes.add("Rewrote YEAR(column) equality into a sargable date range.");
    }
    if (rewritten.toUpperCase().contains("WHERE 1 = 1 AND ")) {
      rewritten = rewritten.replace("WHERE 1 = 1 AND ", "WHERE ");
      changes.add("Removed tautological predicate 1 = 1.");
    }
    return new RewriteResult(original, rewritten, changes);
  }

  private YearPredicate findYearPredicate(List<Token> tokens) {
    for (int i = 0; i < tokens.size() - 5; i++) {
      if (tokens.get(i).value().equalsIgnoreCase("YEAR")
          && tokens.get(i + 1).value().equals("(")
          && SqlTokenUtils.isIdentifier(tokens.get(i + 2))
          && tokens.get(i + 3).value().equals(")")
          && tokens.get(i + 4).value().equals("=")) {
        try {
          int year = Integer.parseInt(tokens.get(i + 5).value());
          String expression = "YEAR (" + tokens.get(i + 2).value() + ") = " + year;
          return new YearPredicate(expression, tokens.get(i + 2).value(), year);
        } catch (NumberFormatException ignored) {
          return null;
        }
      }
    }
    return null;
  }

  private record YearPredicate(String expression, String column, int year) {}
}
