package com.demo.todo.service;

import com.demo.todo.dto.CreateTodoRequest;
import com.demo.todo.dto.TodoResponse;
import com.demo.todo.exception.InvalidTodoInputException;
import com.demo.todo.exception.TodoNotFoundException;
import com.demo.todo.model.Todo;
import com.demo.todo.model.TodoStatus;
import java.time.Clock;
import java.time.Instant;
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
        Todo todo = dataService.findById(id)
                .orElseThrow(() -> new TodoNotFoundException(id));
        return toResponse(todo);
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
