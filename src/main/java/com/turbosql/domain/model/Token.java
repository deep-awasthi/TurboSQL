package com.turbosql.domain.model;

public record Token(TokenType type, String value, int line, int column, int position) {}
