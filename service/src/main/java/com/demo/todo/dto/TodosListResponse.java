package com.demo.todo.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "TodosListResponse", description = "List response containing todos and metadata")
public record TodosListResponse(
        @ArraySchema(arraySchema = @Schema(description = "Returned todo items"), schema = @Schema(implementation = TodoResponse.class))
        List<TodoResponse> items,
        @Schema(description = "List metadata")
        TodosListMeta meta
) {
}
