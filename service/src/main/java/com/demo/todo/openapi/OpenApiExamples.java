package com.demo.todo.openapi;

public final class OpenApiExamples {

    private OpenApiExamples() {
    }

    public static final String CREATE_TODO_REQUEST_EXAMPLE = """
            {
              "description": "Pay rent",
              "dueAt": "2026-03-15T10:00:00Z"
            }
            """;

    public static final String UPDATE_DESCRIPTION_REQUEST_EXAMPLE = """
            {
              "description": "Pay rent (landlord)"
            }
            """;

    public static final String TODO_RESPONSE_NOT_DONE_EXAMPLE = """
            {
              "id": 123,
              "description": "Pay rent",
              "status": "NOT_DONE",
              "createdAt": "2026-03-01T09:30:00Z",
              "dueAt": "2026-03-15T10:00:00Z",
              "doneAt": null
            }
            """;

    public static final String TODO_RESPONSE_DONE_EXAMPLE = """
            {
              "id": 123,
              "description": "Pay rent",
              "status": "DONE",
              "createdAt": "2026-03-01T09:30:00Z",
              "dueAt": "2026-03-15T10:00:00Z",
              "doneAt": "2026-03-01T09:45:12Z"
            }
            """;

    public static final String TODOS_LIST_RESPONSE_EXAMPLE = """
            {
              "items": [
                {
                  "id": 123,
                  "description": "Pay rent",
                  "status": "NOT_DONE",
                  "createdAt": "2026-03-01T09:30:00Z",
                  "dueAt": "2026-03-15T10:00:00Z",
                  "doneAt": null
                }
              ],
              "meta": {
                "count": 1,
                "all": false
              }
            }
            """;

    public static final String ERROR_INVALID_REQUEST_EXAMPLE = """
            {
              "error": "INVALID_REQUEST",
              "message": "description: description must not be blank",
              "path": "/todos",
              "timestamp": "2026-03-01T09:40:00Z"
            }
            """;

    public static final String ERROR_NOT_FOUND_EXAMPLE = """
            {
              "error": "TODO_NOT_FOUND",
              "message": "Todo not found for id: 999",
              "path": "/todos/999",
              "timestamp": "2026-03-01T09:40:00Z"
            }
            """;

    public static final String ERROR_PAST_DUE_EXAMPLE = """
            {
              "error": "PAST_DUE_IMMUTABLE",
              "message": "Past due items cannot be modified.",
              "path": "/todos/123/description",
              "timestamp": "2026-03-01T09:40:00Z"
            }
            """;

    public static final String ERROR_OVERDUE_REOPEN_EXAMPLE = """
            {
              "error": "OVERDUE_REOPEN_FORBIDDEN",
              "message": "Overdue done items cannot be reopened.",
              "path": "/todos/123/not-done",
              "timestamp": "2026-03-01T09:40:00Z"
            }
            """;
}
