package com.demo.todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreateTodoRequest(
        @NotBlank(message = "description must not be blank")
        String description,
        @NotNull(message = "dueAt is required")
        Instant dueAt
) {
}
