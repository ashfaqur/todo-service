package com.demo.todo.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateDescriptionRequest(
        @NotBlank(message = "description must not be blank")
        String description
) {
}
