package com.demo.todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.demo.todo.exception.OverdueReopenForbiddenException;
import com.demo.todo.exception.PastDueImmutableException;
import com.demo.todo.exception.TodoNotFoundException;
import com.demo.todo.model.Todo;
import com.demo.todo.model.TodoStatus;
import com.demo.todo.repository.TodoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataServiceTest {

    @Mock
    private TodoRepository todoRepository;

    private DataService dataService;

    @BeforeEach
    void setUp() {
        dataService = new DataService(todoRepository);
    }

    @Test
    void saveDelegatesToRepository() {
        Todo todo = new Todo();
        Todo saved = new Todo();
        saved.setId(1L);

        when(todoRepository.save(todo)).thenReturn(saved);

        Todo result = dataService.save(todo);

        assertThat(result).isSameAs(saved);
        verify(todoRepository).save(todo);
    }

    @Test
    void findByIdDelegatesToRepository() {
        Todo todo = new Todo();
        todo.setId(5L);
        when(todoRepository.findById(5L)).thenReturn(Optional.of(todo));

        Optional<Todo> result = dataService.findById(5L);

        assertThat(result).contains(todo);
        verify(todoRepository).findById(5L);
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        when(todoRepository.findById(9L)).thenReturn(Optional.empty());

        Optional<Todo> result = dataService.findById(9L);

        assertThat(result).isEmpty();
        verify(todoRepository).findById(9L);
    }

    @Test
    void getByIdWithOverdueSyncRunsUpdateThenFetch() {
        Instant now = Instant.parse("2026-03-01T10:00:00Z");
        Todo todo = new Todo();
        todo.setId(8L);

        when(todoRepository.findById(8L)).thenReturn(Optional.of(todo));

        Optional<Todo> result = dataService.getByIdWithOverdueSync(8L, now);

        assertThat(result).contains(todo);
        verify(todoRepository).markOverdueAsPastDueById(8L, now);
        verify(todoRepository).findById(8L);
    }

    @Test
    void listWithOverdueSyncFalseUpdatesThenReadsNotDone() {
        Instant now = Instant.parse("2026-03-01T10:00:00Z");
        Todo todo = new Todo();
        todo.setId(10L);

        when(todoRepository.findByStatusOrderByCreatedAtAsc(TodoStatus.NOT_DONE)).thenReturn(List.of(todo));

        List<Todo> result = dataService.listWithOverdueSync(false, now);

        assertThat(result).containsExactly(todo);
        verify(todoRepository).markOverdueAsPastDue(now);
        verify(todoRepository).findByStatusOrderByCreatedAtAsc(TodoStatus.NOT_DONE);
    }

    @Test
    void listWithOverdueSyncTrueUpdatesThenReadsAllSorted() {
        Instant now = Instant.parse("2026-03-01T10:00:00Z");
        Todo todo = new Todo();
        todo.setId(11L);

        when(todoRepository.findAllByOrderByCreatedAtAsc()).thenReturn(List.of(todo));

        List<Todo> result = dataService.listWithOverdueSync(true, now);

        assertThat(result).containsExactly(todo);
        verify(todoRepository).markOverdueAsPastDue(now);
        verify(todoRepository).findAllByOrderByCreatedAtAsc();
    }

    @Test
    void syncOverdueDelegatesToRepositoryAndReturnsCount() {
        Instant now = Instant.parse("2026-03-01T10:00:00Z");
        when(todoRepository.markOverdueAsPastDue(now)).thenReturn(3);

        int updated = dataService.syncOverdue(now);

        assertThat(updated).isEqualTo(3);
        verify(todoRepository).markOverdueAsPastDue(now);
    }

    @Test
    void updateDescriptionUpdatesWhenMutable() {
        Instant now = Instant.parse("2026-03-01T10:00:00Z");
        Todo todo = new Todo();
        todo.setId(1L);
        todo.setStatus(TodoStatus.NOT_DONE);
        todo.setDescription("Old");

        when(todoRepository.findById(1L)).thenReturn(Optional.of(todo));
        when(todoRepository.save(todo)).thenReturn(todo);

        Todo result = dataService.updateDescription(1L, "New", now);

        assertThat(result.getDescription()).isEqualTo("New");
        verify(todoRepository).markOverdueAsPastDueById(1L, now);
        verify(todoRepository).save(todo);
    }

    @Test
    void updateDescriptionThrowsNotFoundWhenMissing() {
        Instant now = Instant.parse("2026-03-01T10:00:00Z");
        when(todoRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dataService.updateDescription(3L, "New", now))
                .isInstanceOf(TodoNotFoundException.class);
    }

    @Test
    void updateDescriptionThrowsConflictWhenPastDue() {
        Instant now = Instant.parse("2026-03-01T10:00:00Z");
        Todo todo = new Todo();
        todo.setId(4L);
        todo.setStatus(TodoStatus.PAST_DUE);

        when(todoRepository.findById(4L)).thenReturn(Optional.of(todo));

        assertThatThrownBy(() -> dataService.updateDescription(4L, "X", now))
                .isInstanceOf(PastDueImmutableException.class);
        verify(todoRepository, never()).save(todo);
    }

    @Test
    void markDoneTransitionsNotDoneToDone() {
        Instant now = Instant.parse("2026-03-01T10:00:00Z");
        Todo todo = new Todo();
        todo.setId(5L);
        todo.setStatus(TodoStatus.NOT_DONE);

        when(todoRepository.findById(5L)).thenReturn(Optional.of(todo));
        when(todoRepository.save(todo)).thenReturn(todo);

        Todo result = dataService.markDone(5L, now);

        assertThat(result.getStatus()).isEqualTo(TodoStatus.DONE);
        assertThat(result.getDoneAt()).isEqualTo(now);
    }

    @Test
    void markDoneIsIdempotentWhenAlreadyDone() {
        Instant now = Instant.parse("2026-03-01T10:00:00Z");
        Instant doneAt = Instant.parse("2026-03-01T09:00:00Z");
        Todo todo = new Todo();
        todo.setId(6L);
        todo.setStatus(TodoStatus.DONE);
        todo.setDoneAt(doneAt);

        when(todoRepository.findById(6L)).thenReturn(Optional.of(todo));

        Todo result = dataService.markDone(6L, now);

        assertThat(result.getStatus()).isEqualTo(TodoStatus.DONE);
        assertThat(result.getDoneAt()).isEqualTo(doneAt);
        verify(todoRepository, never()).save(todo);
    }

    @Test
    void markDoneThrowsConflictWhenPastDue() {
        Instant now = Instant.parse("2026-03-01T10:00:00Z");
        Todo todo = new Todo();
        todo.setId(7L);
        todo.setStatus(TodoStatus.PAST_DUE);
        when(todoRepository.findById(7L)).thenReturn(Optional.of(todo));

        assertThatThrownBy(() -> dataService.markDone(7L, now))
                .isInstanceOf(PastDueImmutableException.class);
    }

    @Test
    void markDoneThrowsNotFoundWhenMissing() {
        Instant now = Instant.parse("2026-03-01T10:00:00Z");
        when(todoRepository.findById(50L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dataService.markDone(50L, now))
                .isInstanceOf(TodoNotFoundException.class);
    }

    @Test
    void markNotDoneTransitionsDoneToNotDone() {
        Instant now = Instant.parse("2026-03-01T10:00:00Z");
        Todo todo = new Todo();
        todo.setId(8L);
        todo.setStatus(TodoStatus.DONE);
        todo.setDueAt(now.plusSeconds(30));
        todo.setDoneAt(now.minusSeconds(60));

        when(todoRepository.findById(8L)).thenReturn(Optional.of(todo));
        when(todoRepository.save(todo)).thenReturn(todo);

        Todo result = dataService.markNotDone(8L, now);

        assertThat(result.getStatus()).isEqualTo(TodoStatus.NOT_DONE);
        assertThat(result.getDoneAt()).isNull();
    }

    @Test
    void markNotDoneIsIdempotentWhenAlreadyNotDone() {
        Instant now = Instant.parse("2026-03-01T10:00:00Z");
        Todo todo = new Todo();
        todo.setId(9L);
        todo.setStatus(TodoStatus.NOT_DONE);
        todo.setDueAt(now.plusSeconds(30));
        todo.setDoneAt(null);

        when(todoRepository.findById(9L)).thenReturn(Optional.of(todo));
        when(todoRepository.save(todo)).thenReturn(todo);

        Todo result = dataService.markNotDone(9L, now);

        assertThat(result.getStatus()).isEqualTo(TodoStatus.NOT_DONE);
        assertThat(result.getDoneAt()).isNull();
    }

    @Test
    void markNotDoneThrowsConflictWhenPastDue() {
        Instant now = Instant.parse("2026-03-01T10:00:00Z");
        Todo todo = new Todo();
        todo.setId(10L);
        todo.setStatus(TodoStatus.PAST_DUE);
        when(todoRepository.findById(10L)).thenReturn(Optional.of(todo));

        assertThatThrownBy(() -> dataService.markNotDone(10L, now))
                .isInstanceOf(PastDueImmutableException.class);
    }

    @Test
    void markNotDoneThrowsNotFoundWhenMissing() {
        Instant now = Instant.parse("2026-03-01T10:00:00Z");
        when(todoRepository.findById(51L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dataService.markNotDone(51L, now))
                .isInstanceOf(TodoNotFoundException.class);
    }

    @Test
    void markNotDoneThrowsConflictWhenDoneAndDueAtBeforeNow() {
        Instant now = Instant.parse("2026-03-01T10:00:00Z");
        Todo todo = new Todo();
        todo.setId(11L);
        todo.setStatus(TodoStatus.DONE);
        todo.setDueAt(now.minusSeconds(1));
        todo.setDoneAt(now.minusSeconds(100));
        when(todoRepository.findById(11L)).thenReturn(Optional.of(todo));

        assertThatThrownBy(() -> dataService.markNotDone(11L, now))
                .isInstanceOf(OverdueReopenForbiddenException.class);

        verify(todoRepository).markOverdueAsPastDueById(11L, now);
        verify(todoRepository, never()).save(todo);
    }

    @Test
    void markNotDoneAllowsReopenWhenDoneAndDueAtEqualsNow() {
        Instant now = Instant.parse("2026-03-01T10:00:00Z");
        Todo todo = new Todo();
        todo.setId(12L);
        todo.setStatus(TodoStatus.DONE);
        todo.setDueAt(now);
        todo.setDoneAt(now.minusSeconds(100));
        when(todoRepository.findById(12L)).thenReturn(Optional.of(todo));
        when(todoRepository.save(todo)).thenReturn(todo);

        Todo result = dataService.markNotDone(12L, now);

        assertThat(result.getStatus()).isEqualTo(TodoStatus.NOT_DONE);
        assertThat(result.getDoneAt()).isNull();
        verify(todoRepository).save(todo);
    }
}
