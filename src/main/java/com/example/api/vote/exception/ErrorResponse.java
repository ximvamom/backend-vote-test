package com.example.api.vote.exception;

public record ErrorResponse(
        String code,
        String message
) {
}
