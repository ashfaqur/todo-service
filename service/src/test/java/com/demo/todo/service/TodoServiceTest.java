package com.demo.todo.service;

import com.demo.todo.dto.CreateTodoRequest;
import com.demo.todo.dto.TodoResponse;
import com.demo.todo.dto.TodosListResponse;
import com.demo.todo.dto.UpdateDescriptionRequest;
import com.demo.todo.exception.InvalidTodoInputException;
import com.demo.todo.exception.OverdueReopenForbiddenException;
import com.demo.todo.exception.PastDueImmutableException;
import com.demo.todo.exception.TodoNotFoundException;
import com.demo.todo.model.Todo;
import com.demo.todo.model.TodoStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    void createTodoTrimsDescriptionBeforeSave() {
        Instant now = Instant.now(fixedClock);
        CreateTodoRequest request = new CreateTodoRequest("  Pay rent  ", now.plusSeconds(60));

        Todo savedTodo = new Todo();
        savedTodo.setId(12L);
        savedTodo.setDescription("Pay rent");
        savedTodo.setStatus(TodoStatus.NOT_DONE);
        savedTodo.setCreatedAt(now);
        savedTodo.setDueAt(now.plusSeconds(60));
        savedTodo.setDoneAt(null);

        when(dataService.save(any(Todo.class))).thenReturn(savedTodo);

        todoService.createTodo(request);

        ArgumentCaptor<Todo> captor = ArgumentCaptor.forClass(Todo.class);
        verify(dataService).save(captor.capture());
        assertThat(captor.getValue().getDescription()).isEqualTo("Pay rent");
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

        when(dataService.getByIdWithOverdueSync(3L, now)).thenReturn(Optional.of(todo));

        TodoResponse response = todoService.getTodoById(3L);

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.description()).isEqualTo("Read book");
        assertThat(response.status()).isEqualTo(TodoStatus.NOT_DONE);
        verify(dataService).getByIdWithOverdueSync(3L, now);
    }

    @Test
    void getTodoByIdNotFoundThrowsException() {
        Instant now = Instant.now(fixedClock);
        when(dataService.getByIdWithOverdueSync(99L, now)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> todoService.getTodoById(99L))
                .isInstanceOf(TodoNotFoundException.class)
                .hasMessage("Todo not found for id: 99");
    }

    @Test
    void listTodosFalseBuildsListResponse() {
        Instant now = Instant.now(fixedClock);

        Todo todo = new Todo();
        todo.setId(1L);
        todo.setDescription("Task");
        todo.setStatus(TodoStatus.NOT_DONE);
        todo.setCreatedAt(now.minusSeconds(10));
        todo.setDueAt(now.plusSeconds(10));
        todo.setDoneAt(null);

        when(dataService.listWithOverdueSync(false, now)).thenReturn(java.util.List.of(todo));

        TodosListResponse response = todoService.listTodos(false);

        assertThat(response.meta().all()).isFalse();
        assertThat(response.meta().count()).isEqualTo(1);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().status()).isEqualTo(TodoStatus.NOT_DONE);
        verify(dataService).listWithOverdueSync(false, now);
    }

    @Test
    void listTodosTrueBuildsListResponseWithPastDue() {
        Instant now = Instant.now(fixedClock);

        Todo pastDue = new Todo();
        pastDue.setId(2L);
        pastDue.setDescription("Past due task");
        pastDue.setStatus(TodoStatus.PAST_DUE);
        pastDue.setCreatedAt(now.minusSeconds(20));
        pastDue.setDueAt(now.minusSeconds(5));
        pastDue.setDoneAt(null);

        when(dataService.listWithOverdueSync(eq(true), eq(now))).thenReturn(java.util.List.of(pastDue));

        TodosListResponse response = todoService.listTodos(true);

        assertThat(response.meta().all()).isTrue();
        assertThat(response.meta().count()).isEqualTo(1);
        assertThat(response.items().getFirst().status()).isEqualTo(TodoStatus.PAST_DUE);
        verify(dataService).listWithOverdueSync(true, now);
    }

    @Test
    void updateDescriptionDelegatesWithNowAndMapsResponse() {
        Instant now = Instant.now(fixedClock);
        UpdateDescriptionRequest request = new UpdateDescriptionRequest("Pay rent (landlord)");

        Todo todo = new Todo();
        todo.setId(7L);
        todo.setDescription("Pay rent (landlord)");
        todo.setStatus(TodoStatus.NOT_DONE);
        todo.setCreatedAt(now.minusSeconds(60));
        todo.setDueAt(now.plusSeconds(60));
        todo.setDoneAt(null);

        when(dataService.updateDescription(7L, "Pay rent (landlord)", now)).thenReturn(todo);

        TodoResponse response = todoService.updateDescription(7L, request);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.description()).isEqualTo("Pay rent (landlord)");
        verify(dataService).updateDescription(7L, "Pay rent (landlord)", now);
    }

    @Test
    void updateDescriptionTrimsDescriptionBeforeDelegate() {
        Instant now = Instant.now(fixedClock);
        UpdateDescriptionRequest request = new UpdateDescriptionRequest("  Pay rent (landlord)  ");

        Todo todo = new Todo();
        todo.setId(7L);
        todo.setDescription("Pay rent (landlord)");
        todo.setStatus(TodoStatus.NOT_DONE);
        todo.setCreatedAt(now.minusSeconds(60));
        todo.setDueAt(now.plusSeconds(60));
        todo.setDoneAt(null);

        when(dataService.updateDescription(7L, "Pay rent (landlord)", now)).thenReturn(todo);

        todoService.updateDescription(7L, request);

        verify(dataService).updateDescription(7L, "Pay rent (landlord)", now);
    }

    @Test
    void markDoneDelegatesWithNowAndMapsResponse() {
        Instant now = Instant.now(fixedClock);
        Todo todo = new Todo();
        todo.setId(8L);
        todo.setDescription("Pay rent");
        todo.setStatus(TodoStatus.DONE);
        todo.setCreatedAt(now.minusSeconds(60));
        todo.setDueAt(now.plusSeconds(60));
        todo.setDoneAt(now);

        when(dataService.markDone(8L, now)).thenReturn(todo);

        TodoResponse response = todoService.markDone(8L);

        assertThat(response.status()).isEqualTo(TodoStatus.DONE);
        assertThat(response.doneAt()).isEqualTo(now);
        verify(dataService).markDone(8L, now);
    }

    @Test
    void markDonePropagatesTodoNotFound() {
        Instant now = Instant.now(fixedClock);
        when(dataService.markDone(8L, now)).thenThrow(new TodoNotFoundException(8L));

        assertThatThrownBy(() -> todoService.markDone(8L))
                .isInstanceOf(TodoNotFoundException.class)
                .hasMessage("Todo not found for id: 8");
    }

    @Test
    void markDonePropagatesPastDueImmutable() {
        Instant now = Instant.now(fixedClock);
        when(dataService.markDone(8L, now)).thenThrow(new PastDueImmutableException());

        assertThatThrownBy(() -> todoService.markDone(8L))
                .isInstanceOf(PastDueImmutableException.class)
                .hasMessage("Past due items cannot be modified.");
    }

    @Test
    void markNotDoneDelegatesWithNowAndMapsResponse() {
        Instant now = Instant.now(fixedClock);
        Todo todo = new Todo();
        todo.setId(9L);
        todo.setDescription("Pay rent");
        todo.setStatus(TodoStatus.NOT_DONE);
        todo.setCreatedAt(now.minusSeconds(60));
        todo.setDueAt(now.plusSeconds(60));
        todo.setDoneAt(null);

        when(dataService.markNotDone(9L, now)).thenReturn(todo);

        TodoResponse response = todoService.markNotDone(9L);

        assertThat(response.status()).isEqualTo(TodoStatus.NOT_DONE);
        assertThat(response.doneAt()).isNull();
        verify(dataService).markNotDone(9L, now);
    }

    @Test
    void markNotDonePropagatesOverdueReopenForbidden() {
        Instant now = Instant.now(fixedClock);
        when(dataService.markNotDone(10L, now)).thenThrow(new OverdueReopenForbiddenException());

        assertThatThrownBy(() -> todoService.markNotDone(10L))
                .isInstanceOf(OverdueReopenForbiddenException.class)
                .hasMessage("Overdue done items cannot be reopened.");
    }

    @Test
    void updateDescriptionPropagatesPastDueImmutable() {
        Instant now = Instant.now(fixedClock);
        UpdateDescriptionRequest request = new UpdateDescriptionRequest("Pay rent (landlord)");
        when(dataService.updateDescription(7L, "Pay rent (landlord)", now))
                .thenThrow(new PastDueImmutableException());

        assertThatThrownBy(() -> todoService.updateDescription(7L, request))
                .isInstanceOf(PastDueImmutableException.class)
                .hasMessage("Past due items cannot be modified.");
    }
}
