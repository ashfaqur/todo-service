package com.demo.todo.repository;

import com.demo.todo.model.Todo;
import com.demo.todo.model.TodoStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TodoRepositoryIntegrationTest {

    @Autowired
    private TodoRepository todoRepository;

    @BeforeEach
    void clean() {
        todoRepository.deleteAll();
    }

    @Test
    void markOverdueAsPastDueUpdatesOnlyOverdueNotDone() {
        Instant now = Instant.parse("2026-03-01T10:00:00Z");

        Todo overdueNotDone = saveTodo("overdue-not-done", TodoStatus.NOT_DONE,
                now.minusSeconds(100), now.minusSeconds(1), null);
        Todo futureNotDone = saveTodo("future-not-done", TodoStatus.NOT_DONE,
                now.minusSeconds(90), now.plusSeconds(10), null);
        Todo doneOverdue = saveTodo("done-overdue", TodoStatus.DONE,
                now.minusSeconds(80), now.minusSeconds(2), now.minusSeconds(1));

        int updated = todoRepository.markOverdueAsPastDue(now);

        assertThat(updated).isEqualTo(1);
        assertThat(todoRepository.findById(overdueNotDone.getId())).get().extracting(Todo::getStatus)
                .isEqualTo(TodoStatus.PAST_DUE);
        assertThat(todoRepository.findById(futureNotDone.getId())).get().extracting(Todo::getStatus)
                .isEqualTo(TodoStatus.NOT_DONE);
        assertThat(todoRepository.findById(doneOverdue.getId())).get().extracting(Todo::getStatus)
                .isEqualTo(TodoStatus.DONE);
    }

    @Test
    void markOverdueAsPastDueByIdUpdatesOnlyMatchingRow() {
        Instant now = Instant.parse("2026-03-01T10:00:00Z");

        Todo target = saveTodo("target", TodoStatus.NOT_DONE,
                now.minusSeconds(100), now.minusSeconds(1), null);
        Todo other = saveTodo("other", TodoStatus.NOT_DONE,
                now.minusSeconds(90), now.minusSeconds(1), null);

        int updated = todoRepository.markOverdueAsPastDueById(target.getId(), now);

        assertThat(updated).isEqualTo(1);
        assertThat(todoRepository.findById(target.getId())).get().extracting(Todo::getStatus)
                .isEqualTo(TodoStatus.PAST_DUE);
        assertThat(todoRepository.findById(other.getId())).get().extracting(Todo::getStatus)
                .isEqualTo(TodoStatus.NOT_DONE);
    }

    @Test
    void sortedMethodsReturnAscendingByCreatedAt() {
        Instant base = Instant.parse("2026-03-01T10:00:00Z");

        saveTodo("late", TodoStatus.NOT_DONE, base.plusSeconds(10), base.plusSeconds(20), null);
        saveTodo("early", TodoStatus.NOT_DONE, base.minusSeconds(20), base.plusSeconds(30), null);
        saveTodo("middle", TodoStatus.DONE, base.minusSeconds(5), base.plusSeconds(40), base);

        List<Todo> allSorted = todoRepository.findAllByOrderByCreatedAtAsc();
        assertThat(allSorted).extracting(Todo::getDescription)
                .containsExactly("early", "middle", "late");

        List<Todo> notDoneSorted = todoRepository.findByStatusOrderByCreatedAtAsc(TodoStatus.NOT_DONE);
        assertThat(notDoneSorted).extracting(Todo::getDescription)
                .containsExactly("early", "late");
    }

    private Todo saveTodo(String description, TodoStatus status, Instant createdAt, Instant dueAt, Instant doneAt) {
        Todo todo = new Todo();
        todo.setDescription(description);
        todo.setStatus(status);
        todo.setCreatedAt(createdAt);
        todo.setDueAt(dueAt);
        todo.setDoneAt(doneAt);
        return todoRepository.save(todo);
    }
}
