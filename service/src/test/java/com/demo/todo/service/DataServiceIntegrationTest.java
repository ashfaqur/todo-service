package com.demo.todo.service;

import com.demo.todo.exception.OverdueReopenForbiddenException;
import com.demo.todo.exception.PastDueImmutableException;
import com.demo.todo.model.Todo;
import com.demo.todo.model.TodoStatus;
import com.demo.todo.repository.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void getByIdWithOverdueSyncTransitionsOverdueNotDone() {
        Instant now = Instant.parse("2026-03-01T10:00:00Z");
        Todo todo = new Todo();
        todo.setDescription("Task");
        todo.setStatus(TodoStatus.NOT_DONE);
        todo.setCreatedAt(now.minusSeconds(100));
        todo.setDueAt(now.minusSeconds(1));
        todo.setDoneAt(null);
        Todo saved = todoRepository.save(todo);

        Todo result = dataService.getByIdWithOverdueSync(saved.getId(), now).orElseThrow();

        assertThat(result.getStatus()).isEqualTo(TodoStatus.PAST_DUE);
        Todo refreshed = todoRepository.findById(saved.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo(TodoStatus.PAST_DUE);
    }

    @Test
    void listWithOverdueSyncFalseExcludesTransitionedPastDue() {
        Instant now = Instant.parse("2026-03-01T10:00:00Z");

        Todo overdueNotDone = new Todo();
        overdueNotDone.setDescription("Overdue");
        overdueNotDone.setStatus(TodoStatus.NOT_DONE);
        overdueNotDone.setCreatedAt(now.minusSeconds(100));
        overdueNotDone.setDueAt(now.minusSeconds(1));
        overdueNotDone.setDoneAt(null);
        todoRepository.save(overdueNotDone);

        Todo futureNotDone = new Todo();
        futureNotDone.setDescription("Future");
        futureNotDone.setStatus(TodoStatus.NOT_DONE);
        futureNotDone.setCreatedAt(now.minusSeconds(90));
        futureNotDone.setDueAt(now.plusSeconds(60));
        futureNotDone.setDoneAt(null);
        Todo futureSaved = todoRepository.save(futureNotDone);

        List<Todo> result = dataService.listWithOverdueSync(false, now);

        assertThat(result).extracting(Todo::getId).containsExactly(futureSaved.getId());
        assertThat(todoRepository.findAllByOrderByCreatedAtAsc())
                .filteredOn(t -> "Overdue".equals(t.getDescription()))
                .first()
                .extracting(Todo::getStatus)
                .isEqualTo(TodoStatus.PAST_DUE);
    }

    @Test
    void listWithOverdueSyncTrueIncludesPastDueAfterSync() {
        Instant now = Instant.parse("2026-03-01T10:00:00Z");

        Todo overdueNotDone = new Todo();
        overdueNotDone.setDescription("Overdue");
        overdueNotDone.setStatus(TodoStatus.NOT_DONE);
        overdueNotDone.setCreatedAt(now.minusSeconds(100));
        overdueNotDone.setDueAt(now.minusSeconds(1));
        overdueNotDone.setDoneAt(null);
        Todo overdueSaved = todoRepository.save(overdueNotDone);

        Todo done = new Todo();
        done.setDescription("Done");
        done.setStatus(TodoStatus.DONE);
        done.setCreatedAt(now.minusSeconds(90));
        done.setDueAt(now.minusSeconds(30));
        done.setDoneAt(now.minusSeconds(60));
        Todo doneSaved = todoRepository.save(done);

        List<Todo> result = dataService.listWithOverdueSync(true, now);

        assertThat(result).extracting(Todo::getId).contains(overdueSaved.getId(), doneSaved.getId());
        assertThat(result).filteredOn(t -> t.getId().equals(overdueSaved.getId()))
                .first()
                .extracting(Todo::getStatus)
                .isEqualTo(TodoStatus.PAST_DUE);
    }
}
