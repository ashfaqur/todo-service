package com.demo.todo.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * DTO representing a todo list API response.
 *
 * @param items returned todo items
 * @param meta list metadata
 */
@Schema(name = "TodosListResponse", description = "List response containing todos and metadata")
public record TodosListResponse(
        @ArraySchema(arraySchema = @Schema(description = "Returned todo items"), schema = @Schema(implementation = TodoResponse.class))
        List<TodoResponse> items,
        @Schema(description = "List metadata")
        TodosListMeta meta
) {
}
