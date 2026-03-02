package com.demo.todo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(name = "ErrorResponse", description = "Standard error payload returned by API failures")
public record ErrorResponse(
        @Schema(description = "Machine-readable error code", example = "INVALID_REQUEST")
        String error,
        @Schema(description = "Human-readable error message", example = "description: must not be blank")
        String message,
        @Schema(description = "Request path where the error occurred", example = "/todos/123/description")
        String path,
        @Schema(description = "Error timestamp in UTC", example = "2026-03-01T09:40:00Z")
        Instant timestamp
) {
}
