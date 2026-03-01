package com.demo.todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.demo.todo.dto.CreateTodoRequest;
import com.demo.todo.dto.TodoResponse;
import com.demo.todo.exception.InvalidTodoInputException;
import com.demo.todo.exception.TodoNotFoundException;
import com.demo.todo.model.Todo;
import com.demo.todo.model.TodoStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    @Mock
    private DataService dataService;

    private TodoService todoService;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-03-01T10:00:00Z"), ZoneOffset.UTC);
        todoService = new TodoService(dataService, fixedClock);
    }

    @Test
    void createTodoSuccessWhenDueAtEqualsNow() {
        Instant now = Instant.now(fixedClock);
        CreateTodoRequest request = new CreateTodoRequest("Pay rent", now);

        Todo savedTodo = new Todo();
        savedTodo.setId(1L);
        savedTodo.setDescription("Pay rent");
        savedTodo.setStatus(TodoStatus.NOT_DONE);
        savedTodo.setCreatedAt(now);
        savedTodo.setDueAt(now);
        savedTodo.setDoneAt(null);

        when(dataService.save(any(Todo.class))).thenReturn(savedTodo);

        TodoResponse response = todoService.createTodo(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.description()).isEqualTo("Pay rent");
        assertThat(response.status()).isEqualTo(TodoStatus.NOT_DONE);
        assertThat(response.createdAt()).isEqualTo(now);
        assertThat(response.dueAt()).isEqualTo(now);
        assertThat(response.doneAt()).isNull();
        verify(dataService).save(any(Todo.class));
    }

    @Test
    void createTodoSuccessWhenDueAtAfterNow() {
        Instant now = Instant.now(fixedClock);
        Instant dueAt = now.plusSeconds(3600);
        CreateTodoRequest request = new CreateTodoRequest("Buy milk", dueAt);

        Todo savedTodo = new Todo();
        savedTodo.setId(2L);
        savedTodo.setDescription("Buy milk");
        savedTodo.setStatus(TodoStatus.NOT_DONE);
        savedTodo.setCreatedAt(now);
        savedTodo.setDueAt(dueAt);
        savedTodo.setDoneAt(null);

        when(dataService.save(any(Todo.class))).thenReturn(savedTodo);

        TodoResponse response = todoService.createTodo(request);

        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.createdAt()).isEqualTo(now);
        assertThat(response.dueAt()).isEqualTo(dueAt);
        assertThat(response.status()).isEqualTo(TodoStatus.NOT_DONE);
    }

    @Test
    void createTodoThrowsInvalidTodoInputWhenDueAtBeforeNow() {
        Instant dueAt = Instant.now(fixedClock).minusSeconds(1);
        CreateTodoRequest request = new CreateTodoRequest("Late task", dueAt);

        assertThatThrownBy(() -> todoService.createTodo(request))
                .isInstanceOf(InvalidTodoInputException.class)
                .hasMessage("dueAt must not be before current time");
    }

    @Test
    void getTodoByIdSuccess() {
        Instant now = Instant.now(fixedClock);

        Todo todo = new Todo();
        todo.setId(3L);
        todo.setDescription("Read book");
        todo.setStatus(TodoStatus.NOT_DONE);
        todo.setCreatedAt(now);
        todo.setDueAt(now.plusSeconds(10));

        when(dataService.findById(3L)).thenReturn(Optional.of(todo));

        TodoResponse response = todoService.getTodoById(3L);

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.description()).isEqualTo("Read book");
        assertThat(response.status()).isEqualTo(TodoStatus.NOT_DONE);
    }

    @Test
    void getTodoByIdNotFoundThrowsException() {
        when(dataService.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> todoService.getTodoById(99L))
                .isInstanceOf(TodoNotFoundException.class)
                .hasMessage("Todo not found for id: 99");
    }
}
