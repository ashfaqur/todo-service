package com.demo.todo.dto;

import java.util.List;

public record TodosListResponse(
        List<TodoResponse> items,
        TodosListMeta meta
) {
}
