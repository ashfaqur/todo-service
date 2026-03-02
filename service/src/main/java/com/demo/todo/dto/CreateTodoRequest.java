package com.demo.todo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Schema(name = "CreateTodoRequest", description = "Payload for creating a new todo item")
public record CreateTodoRequest(
        @NotBlank(message = "description must not be blank")
        @Schema(description = "Human-readable todo description", example = "Pay rent", requiredMode = Schema.RequiredMode.REQUIRED)
        String description,
        @NotNull(message = "dueAt is required")
        @Schema(description = "Due timestamp in ISO-8601 UTC format", example = "2026-03-15T10:00:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant dueAt
) {
}
