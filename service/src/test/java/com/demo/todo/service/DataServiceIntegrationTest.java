package com.demo.todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.demo.todo.exception.OverdueReopenForbiddenException;
import com.demo.todo.exception.PastDueImmutableException;
import com.demo.todo.model.Todo;
import com.demo.todo.model.TodoStatus;
import com.demo.todo.repository.TodoRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DataServiceIntegrationTest {

    @Autowired
    private DataService dataService;

    @Autowired
    private TodoRepository todoRepository;

    @BeforeEach
    void setup() {
        todoRepository.deleteAll();
    }

    @Test
    void overdueNotDoneWriteIsRejectedAfterSync() {
        Instant now = Instant.parse("2026-03-01T10:00:00Z");
        Todo todo = new Todo();
        todo.setDescription("Task");
        todo.setStatus(TodoStatus.NOT_DONE);
        todo.setCreatedAt(now.minusSeconds(100));
        todo.setDueAt(now.minusSeconds(10));
        todo.setDoneAt(null);
        Todo saved = todoRepository.save(todo);

        assertThatThrownBy(() -> dataService.updateDescription(saved.getId(), "Updated", now))
                .isInstanceOf(PastDueImmutableException.class);

        Todo refreshed = todoRepository.findById(saved.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo(TodoStatus.PAST_DUE);
    }

    @Test
    void doneOverdueCannotBeReopened() {
        Instant now = Instant.parse("2026-03-01T10:00:00Z");
        Todo todo = new Todo();
        todo.setDescription("Task");
        todo.setStatus(TodoStatus.DONE);
        todo.setCreatedAt(now.minusSeconds(100));
        todo.setDueAt(now.minusSeconds(10));
        todo.setDoneAt(now.minusSeconds(50));
        Todo saved = todoRepository.save(todo);

        assertThatThrownBy(() -> dataService.markNotDone(saved.getId(), now))
                .isInstanceOf(OverdueReopenForbiddenException.class);

        Todo refreshed = todoRepository.findById(saved.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo(TodoStatus.DONE);
        assertThat(refreshed.getDoneAt()).isEqualTo(now.minusSeconds(50));
    }

    @Test
    void doneAtBoundaryDueAtEqualsNowCanBeReopened() {
        Instant now = Instant.parse("2026-03-01T10:00:00Z");
        Todo todo = new Todo();
        todo.setDescription("Task");
        todo.setStatus(TodoStatus.DONE);
        todo.setCreatedAt(now.minusSeconds(100));
        todo.setDueAt(now);
        todo.setDoneAt(now.minusSeconds(50));
        Todo saved = todoRepository.save(todo);

        Todo updated = dataService.markNotDone(saved.getId(), now);

        assertThat(updated.getStatus()).isEqualTo(TodoStatus.NOT_DONE);
        assertThat(updated.getDoneAt()).isNull();
    }

    @Test
    void nonOverdueNotDoneCanBeMarkedDone() {
        Instant now = Instant.parse("2026-03-01T10:00:00Z");
        Todo todo = new Todo();
        todo.setDescription("Task");
        todo.setStatus(TodoStatus.NOT_DONE);
        todo.setCreatedAt(now.minusSeconds(100));
        todo.setDueAt(now.plusSeconds(60));
        todo.setDoneAt(null);
        Todo saved = todoRepository.save(todo);

        Todo updated = dataService.markDone(saved.getId(), now);

        assertThat(updated.getStatus()).isEqualTo(TodoStatus.DONE);
        assertThat(updated.getDoneAt()).isEqualTo(now);
    }
}
