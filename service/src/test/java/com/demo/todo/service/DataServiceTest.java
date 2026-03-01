package com.demo.todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
}
