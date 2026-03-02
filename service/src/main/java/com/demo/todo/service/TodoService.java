package com.demo.todo.service;

import com.demo.todo.dto.*;
import com.demo.todo.exception.InvalidTodoInputException;
import com.demo.todo.exception.TodoNotFoundException;
import com.demo.todo.model.Todo;
import com.demo.todo.model.TodoStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Service-layer orchestrator for Todo API use cases.
 * <p>
 * Applies request-time business rules and delegates transactional persistence
 * operations to {@link DataService}.
 */
@Service
public class TodoService {

    private final DataService dataService;
    private final Clock clock;

    /**
     * Creates a new service instance.
     *
     * @param dataService data access/service boundary for persistence operations
     * @param clock       clock used to evaluate and stamp time-dependent behavior
     */
    public TodoService(DataService dataService, Clock clock) {
        this.dataService = dataService;
        this.clock = clock;
    }

    /**
     * Creates a new todo in {@code NOT_DONE} state.
     *
     * @param request create request payload
     * @return created todo mapped for API response
     * @throws InvalidTodoInputException when {@code dueAt} is before current time
     */
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

    /**
     * Reads a single todo by id after overdue synchronization.
     *
     * @param id todo identifier
     * @return todo mapped for API response
     * @throws TodoNotFoundException when no todo exists for the given id
     */
    public TodoResponse getTodoById(Long id) {
        Instant now = Instant.now(clock);
        Todo todo = dataService.getByIdWithOverdueSync(id, now)
                .orElseThrow(() -> new TodoNotFoundException(id));
        return toResponse(todo);
    }

    /**
     * Lists todos with optional inclusion of all statuses.
     *
     * @param all when {@code true}, returns all statuses; otherwise returns only {@code NOT_DONE}
     * @return list response with items and summary metadata
     */
    public TodosListResponse listTodos(boolean all) {
        Instant now = Instant.now(clock);
        List<TodoResponse> items = dataService.listWithOverdueSync(all, now).stream()
                .map(this::toResponse)
                .toList();
        TodosListMeta meta = new TodosListMeta(items.size(), all);
        return new TodosListResponse(items, meta);
    }

    /**
     * Updates todo description after state synchronization and guards.
     *
     * @param id      todo identifier
     * @param request description update payload
     * @return updated todo mapped for API response
     */
    public TodoResponse updateDescription(Long id, UpdateDescriptionRequest request) {
        Instant now = Instant.now(clock);
        Todo updated = dataService.updateDescription(id, request.description().trim(), now);
        return toResponse(updated);
    }

    /**
     * Marks a todo as done.
     *
     * @param id todo identifier
     * @return resulting todo mapped for API response
     */
    public TodoResponse markDone(Long id) {
        Instant now = Instant.now(clock);
        Todo updated = dataService.markDone(id, now);
        return toResponse(updated);
    }

    /**
     * Marks a todo as not done.
     *
     * @param id todo identifier
     * @return resulting todo mapped for API response
     */
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
