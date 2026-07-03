package com.turbosql.dto;

public record ErrorEnvelope(boolean success, ErrorPayload error) {

  public static ErrorEnvelope of(String code, String message, Integer line, Integer column) {
    return new ErrorEnvelope(false, new ErrorPayload(code, message, line, column));
  }
}
