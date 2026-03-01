package com.demo.todo.controller;

import com.demo.todo.dto.CreateTodoRequest;
import com.demo.todo.dto.TodoResponse;
import com.demo.todo.service.TodoService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TodoRestController {

    private final TodoService todoService;

    public TodoRestController(TodoService todoService) {
        this.todoService = todoService;
    }

    @PostMapping("/todos")
    public ResponseEntity<TodoResponse> createTodo(@Valid @RequestBody CreateTodoRequest request) {
        TodoResponse response = todoService.createTodo(request);
        URI location = URI.create("/todos/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/todos/{id}")
    public ResponseEntity<TodoResponse> getTodoById(@PathVariable Long id) {
        TodoResponse response = todoService.getTodoById(id);
        return ResponseEntity.ok(response);
    }
}
