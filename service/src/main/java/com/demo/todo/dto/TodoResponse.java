package com.demo.todo.dto;

import com.demo.todo.model.TodoStatus;
import java.time.Instant;

public record TodoResponse(
        Long id,
        String description,
        TodoStatus status,
        Instant createdAt,
        Instant dueAt,
        Instant doneAt
) {
}
