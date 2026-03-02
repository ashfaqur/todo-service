package com.demo.todo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "UpdateDescriptionRequest", description = "Payload for updating a todo description")
public record UpdateDescriptionRequest(
        @NotBlank(message = "description must not be blank")
        @Size(max = 1000, message = "description must be at most 1000 characters")
        @Schema(description = "Updated todo description", example = "Pay rent (landlord)", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 1000)
        String description
) {
}
