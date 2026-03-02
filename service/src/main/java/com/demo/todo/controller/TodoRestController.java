package com.demo.todo.controller;

import com.demo.todo.dto.CreateTodoRequest;
import com.demo.todo.dto.TodoResponse;
import com.demo.todo.dto.TodosListResponse;
import com.demo.todo.dto.UpdateDescriptionRequest;
import com.demo.todo.openapi.TodoApi;
import com.demo.todo.service.TodoService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/todos")
public class TodoRestController implements TodoApi {

    private final TodoService todoService;

    public TodoRestController(TodoService todoService) {
        this.todoService = todoService;
    }

    @Override
    @PostMapping()
    public ResponseEntity<TodoResponse> createTodo(@Valid @RequestBody CreateTodoRequest request) {
        TodoResponse response = todoService.createTodo(request);
        URI location = URI.create("/todos/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<TodoResponse> getTodoById(@PathVariable Long id) {
        TodoResponse response = todoService.getTodoById(id);
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping
    public ResponseEntity<TodosListResponse> listTodos(@RequestParam(defaultValue = "false") boolean all) {
        TodosListResponse response = todoService.listTodos(all);
        return ResponseEntity.ok(response);
    }

    @Override
    @PatchMapping("/{id}/description")
    public ResponseEntity<TodoResponse> updateDescription(@PathVariable Long id,
                                                          @Valid @RequestBody UpdateDescriptionRequest request) {
        TodoResponse response = todoService.updateDescription(id, request);
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/{id}/done")
    public ResponseEntity<TodoResponse> markDone(@PathVariable Long id) {
        TodoResponse response = todoService.markDone(id);
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/{id}/not-done")
    public ResponseEntity<TodoResponse> markNotDone(@PathVariable Long id) {
        TodoResponse response = todoService.markNotDone(id);
        return ResponseEntity.ok(response);
    }
}
