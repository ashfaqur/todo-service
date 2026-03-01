package com.demo.todo.service;

import com.demo.todo.dto.CreateTodoRequest;
import com.demo.todo.dto.TodoResponse;
import com.demo.todo.dto.UpdateDescriptionRequest;
import com.demo.todo.dto.TodosListMeta;
import com.demo.todo.dto.TodosListResponse;
import com.demo.todo.exception.InvalidTodoInputException;
import com.demo.todo.exception.TodoNotFoundException;
import com.demo.todo.model.Todo;
import com.demo.todo.model.TodoStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TodoService {

    private final DataService dataService;
    private final Clock clock;

    public TodoService(DataService dataService, Clock clock) {
        this.dataService = dataService;
        this.clock = clock;
    }

    public TodoResponse createTodo(CreateTodoRequest request) {
        Instant now = Instant.now(clock);
        if (request.dueAt().isBefore(now)) {
            throw new InvalidTodoInputException("dueAt must not be before current time");
        }

        Todo todo = new Todo();
        todo.setDescription(request.description().trim());
        todo.setStatus(TodoStatus.NOT_DONE);
        todo.setCreatedAt(now);
        todo.setDueAt(request.dueAt());
        todo.setDoneAt(null);

        Todo savedTodo = dataService.save(todo);
        return toResponse(savedTodo);
    }

    public TodoResponse getTodoById(Long id) {
        Instant now = Instant.now(clock);
        Todo todo = dataService.getByIdWithOverdueSync(id, now)
                .orElseThrow(() -> new TodoNotFoundException(id));
        return toResponse(todo);
    }

    public TodosListResponse listTodos(boolean all) {
        Instant now = Instant.now(clock);
        List<TodoResponse> items = dataService.listWithOverdueSync(all, now).stream()
                .map(this::toResponse)
                .toList();
        TodosListMeta meta = new TodosListMeta(items.size(), all);
        return new TodosListResponse(items, meta);
    }

    public TodoResponse updateDescription(Long id, UpdateDescriptionRequest request) {
        Instant now = Instant.now(clock);
        Todo updated = dataService.updateDescription(id, request.description().trim(), now);
        return toResponse(updated);
    }

    public TodoResponse markDone(Long id) {
        Instant now = Instant.now(clock);
        Todo updated = dataService.markDone(id, now);
        return toResponse(updated);
    }

    public TodoResponse markNotDone(Long id) {
        Instant now = Instant.now(clock);
        Todo updated = dataService.markNotDone(id, now);
        return toResponse(updated);
    }

    private TodoResponse toResponse(Todo todo) {
        return new TodoResponse(
                todo.getId(),
                todo.getDescription(),
                todo.getStatus(),
                todo.getCreatedAt(),
                todo.getDueAt(),
                todo.getDoneAt()
        );
    }
}
