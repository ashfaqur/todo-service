package com.demo.todo.openapi;

import static com.demo.todo.openapi.OpenApiExamples.CREATE_TODO_REQUEST_EXAMPLE;
import static com.demo.todo.openapi.OpenApiExamples.ERROR_INVALID_REQUEST_EXAMPLE;
import static com.demo.todo.openapi.OpenApiExamples.ERROR_NOT_FOUND_EXAMPLE;
import static com.demo.todo.openapi.OpenApiExamples.ERROR_OVERDUE_REOPEN_EXAMPLE;
import static com.demo.todo.openapi.OpenApiExamples.ERROR_PAST_DUE_EXAMPLE;
import static com.demo.todo.openapi.OpenApiExamples.TODOS_LIST_RESPONSE_EXAMPLE;
import static com.demo.todo.openapi.OpenApiExamples.TODO_RESPONSE_DONE_EXAMPLE;
import static com.demo.todo.openapi.OpenApiExamples.TODO_RESPONSE_NOT_DONE_EXAMPLE;
import static com.demo.todo.openapi.OpenApiExamples.UPDATE_DESCRIPTION_REQUEST_EXAMPLE;

import com.demo.todo.dto.CreateTodoRequest;
import com.demo.todo.dto.ErrorResponse;
import com.demo.todo.dto.TodoResponse;
import com.demo.todo.dto.TodosListResponse;
import com.demo.todo.dto.UpdateDescriptionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Todos", description = "Operations for creating, listing, and updating todo items")
public interface TodoApi {

    @Operation(
            summary = "Create todo",
            description = "Creates a todo in NOT_DONE state. The dueAt value must be equal to or later than the current time."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Todo created",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TodoResponse.class),
                            examples = @ExampleObject(name = "created", value = TODO_RESPONSE_NOT_DONE_EXAMPLE)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation or input error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "invalidRequest", value = ERROR_INVALID_REQUEST_EXAMPLE)
                    )
            )
    })
    ResponseEntity<TodoResponse> createTodo(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Todo creation payload",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreateTodoRequest.class),
                            examples = @ExampleObject(name = "createRequest", value = CREATE_TODO_REQUEST_EXAMPLE)
                    )
            )
            @Valid @RequestBody CreateTodoRequest request);

    @Operation(
            summary = "Get todo by id",
            description = "Returns a single todo item by its id. If the item is overdue and still NOT_DONE, it is synchronized before returning."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Todo found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TodoResponse.class),
                            examples = @ExampleObject(name = "todo", value = TODO_RESPONSE_NOT_DONE_EXAMPLE)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid path parameter",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "invalidRequest", value = ERROR_INVALID_REQUEST_EXAMPLE)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Todo not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "notFound", value = ERROR_NOT_FOUND_EXAMPLE)
                    )
            )
    })
    ResponseEntity<TodoResponse> getTodoById(
            @Parameter(in = ParameterIn.PATH, description = "Todo identifier", example = "123")
            @PathVariable Long id);

    @Operation(
            summary = "List todos",
            description = "Returns todo items sorted by creation time. Use all=true to include DONE and PAST_DUE items."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "List returned",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TodosListResponse.class),
                            examples = @ExampleObject(name = "list", value = TODOS_LIST_RESPONSE_EXAMPLE)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid query parameter",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "invalidRequest", value = ERROR_INVALID_REQUEST_EXAMPLE)
                    )
            )
    })
    ResponseEntity<TodosListResponse> listTodos(
            @Parameter(in = ParameterIn.QUERY,
                    description = "If true, return all statuses. If false (default), return NOT_DONE only.",
                    example = "false")
            @RequestParam(defaultValue = "false") boolean all);

    @Operation(
            summary = "Update todo description",
            description = "Updates a todo description after overdue synchronization. PAST_DUE items are immutable."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Description updated",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TodoResponse.class),
                            examples = @ExampleObject(name = "updated", value = TODO_RESPONSE_NOT_DONE_EXAMPLE)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation or input error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "invalidRequest", value = ERROR_INVALID_REQUEST_EXAMPLE)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Todo not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "notFound", value = ERROR_NOT_FOUND_EXAMPLE)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict: todo is immutable",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "pastDue", value = ERROR_PAST_DUE_EXAMPLE)
                    )
            )
    })
    ResponseEntity<TodoResponse> updateDescription(
            @Parameter(in = ParameterIn.PATH, description = "Todo identifier", example = "123")
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Updated description payload",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdateDescriptionRequest.class),
                            examples = @ExampleObject(name = "updateRequest", value = UPDATE_DESCRIPTION_REQUEST_EXAMPLE)
                    )
            )
            @Valid @RequestBody UpdateDescriptionRequest request);

    @Operation(
            summary = "Mark todo as done",
            description = "Marks a todo as DONE. If already DONE, the operation is idempotent. PAST_DUE items return conflict."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Todo marked as done",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TodoResponse.class),
                            examples = @ExampleObject(name = "done", value = TODO_RESPONSE_DONE_EXAMPLE)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid path parameter",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "invalidRequest", value = ERROR_INVALID_REQUEST_EXAMPLE)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Todo not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "notFound", value = ERROR_NOT_FOUND_EXAMPLE)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict: todo is immutable",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "pastDue", value = ERROR_PAST_DUE_EXAMPLE)
                    )
            )
    })
    ResponseEntity<TodoResponse> markDone(
            @Parameter(in = ParameterIn.PATH, description = "Todo identifier", example = "123")
            @PathVariable Long id);

    @Operation(
            summary = "Mark todo as not done",
            description = "Marks a todo as NOT_DONE. Reopening overdue DONE items is forbidden and returns conflict."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Todo marked as not done",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TodoResponse.class),
                            examples = @ExampleObject(name = "notDone", value = TODO_RESPONSE_NOT_DONE_EXAMPLE)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid path parameter",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "invalidRequest", value = ERROR_INVALID_REQUEST_EXAMPLE)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Todo not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "notFound", value = ERROR_NOT_FOUND_EXAMPLE)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict: immutable todo or overdue DONE reopen attempt",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "pastDue", value = ERROR_PAST_DUE_EXAMPLE),
                                    @ExampleObject(name = "overdueReopen", value = ERROR_OVERDUE_REOPEN_EXAMPLE)
                            }
                    )
            )
    })
    ResponseEntity<TodoResponse> markNotDone(
            @Parameter(in = ParameterIn.PATH, description = "Todo identifier", example = "123")
            @PathVariable Long id);
}
