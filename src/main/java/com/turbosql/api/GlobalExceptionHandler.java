package com.turbosql.api;

import com.turbosql.common.exception.SqlSyntaxException;
import com.turbosql.dto.ErrorEnvelope;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(SqlSyntaxException.class)
  public ResponseEntity<ErrorEnvelope> handleSyntax(SqlSyntaxException exception) {
    return ResponseEntity.badRequest()
        .body(ErrorEnvelope.of("SQL_SYNTAX_ERROR", exception.getMessage(), exception.line(), exception.column()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorEnvelope> handleValidation(MethodArgumentNotValidException exception) {
    String message =
        exception.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + " " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));
    return ResponseEntity.badRequest().body(ErrorEnvelope.of("VALIDATION_ERROR", message, null, null));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorEnvelope> handleUnreadable(HttpMessageNotReadableException exception) {
    return ResponseEntity.badRequest().body(ErrorEnvelope.of("INVALID_JSON", "Request body must be valid JSON", null, null));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorEnvelope> handleUnexpected(Exception exception) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorEnvelope.of("INTERNAL_ERROR", "Unexpected server error", null, null));
  }
}
