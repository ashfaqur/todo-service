package com.demo.todo.dto;

import com.demo.todo.model.TodoStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(name = "TodoResponse", description = "Todo item returned by the API")
public record TodoResponse(
        @Schema(description = "Todo identifier", example = "123")
        Long id,
        @Schema(description = "Todo description", example = "Pay rent")
        String description,
        @Schema(description = "Current todo lifecycle status", example = "NOT_DONE")
        TodoStatus status,
        @Schema(description = "Creation timestamp in UTC", example = "2026-03-01T09:30:00Z")
        Instant createdAt,
        @Schema(description = "Due timestamp in UTC", example = "2026-03-15T10:00:00Z")
        Instant dueAt,
        @Schema(description = "Completion timestamp in UTC, null unless status is DONE", example = "2026-03-01T09:45:12Z", nullable = true)
        Instant doneAt
) {
}
