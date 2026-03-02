package com.demo.todo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO containing metadata for todo list responses.
 *
 * @param count number of items returned
 * @param all whether the request included all statuses
 */
@Schema(name = "TodosListMeta", description = "Metadata for list responses")
public record TodosListMeta(
        @Schema(description = "Number of items returned", example = "2")
        int count,
        @Schema(description = "Whether all statuses were requested", example = "false")
        boolean all
) {
}
