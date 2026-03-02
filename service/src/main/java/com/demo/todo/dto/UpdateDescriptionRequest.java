package com.demo.todo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "UpdateDescriptionRequest", description = "Payload for updating a todo description")
public record UpdateDescriptionRequest(
        @NotBlank(message = "description must not be blank")
        @Schema(description = "Updated todo description", example = "Pay rent (landlord)", requiredMode = Schema.RequiredMode.REQUIRED)
        String description
) {
}
