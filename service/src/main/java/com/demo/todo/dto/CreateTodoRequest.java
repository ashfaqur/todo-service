package com.demo.todo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * DTO for todo creation requests at the API boundary.
 *
 * @param description todo description provided by the client
 * @param dueAt due timestamp for the todo
 */
@Schema(name = "CreateTodoRequest", description = "Payload for creating a new todo item")
public record CreateTodoRequest(
        @NotBlank(message = "description must not be blank")
        @Size(max = 1000, message = "description must be at most 1000 characters")
        @Schema(description = "Human-readable todo description", example = "Pay rent", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 1000)
        String description,
        @NotNull(message = "dueAt is required")
        @Schema(description = "Due timestamp in ISO-8601 UTC format", example = "2026-03-15T10:00:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant dueAt
) {
}
