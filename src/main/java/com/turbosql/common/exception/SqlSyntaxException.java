package com.turbosql.common.exception;

public class SqlSyntaxException extends RuntimeException {

  private final int line;
  private final int column;

  public SqlSyntaxException(String message, int line, int column) {
    super(message);
    this.line = line;
    this.column = column;
  }

  public int line() {
    return line;
  }

  public int column() {
    return column;
  }
}
