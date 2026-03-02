package com.demo.todo.scheduler;

import com.demo.todo.model.Todo;
import com.demo.todo.model.TodoStatus;
import com.demo.todo.repository.TodoRepository;
import com.demo.todo.service.DataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(OverdueTodoSchedulerIntegrationTest.ClockTestConfig.class)
class OverdueTodoSchedulerIntegrationTest {

    @Autowired
    private DataService dataService;

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private Clock clock;

    @BeforeEach
    void setup() {
        todoRepository.deleteAll();
    }

    @Test
    void runOnceTransitionsOnlyOverdueNotDone() {
        Instant now = Instant.now(clock);

        Todo overdueNotDone = new Todo();
        overdueNotDone.setDescription("Overdue not done");
        overdueNotDone.setStatus(TodoStatus.NOT_DONE);
        overdueNotDone.setCreatedAt(now.minusSeconds(120));
        overdueNotDone.setDueAt(now.minusSeconds(5));
        overdueNotDone.setDoneAt(null);
        Todo overdueSaved = todoRepository.save(overdueNotDone);

        Todo futureNotDone = new Todo();
        futureNotDone.setDescription("Future not done");
        futureNotDone.setStatus(TodoStatus.NOT_DONE);
        futureNotDone.setCreatedAt(now.minusSeconds(110));
        futureNotDone.setDueAt(now.plusSeconds(60));
        futureNotDone.setDoneAt(null);
        Todo futureSaved = todoRepository.save(futureNotDone);

        Todo doneOverdue = new Todo();
        doneOverdue.setDescription("Done overdue");
        doneOverdue.setStatus(TodoStatus.DONE);
        doneOverdue.setCreatedAt(now.minusSeconds(100));
        doneOverdue.setDueAt(now.minusSeconds(10));
        doneOverdue.setDoneAt(now.minusSeconds(20));
        Todo doneSaved = todoRepository.save(doneOverdue);

        OverdueTodoScheduler scheduler = new OverdueTodoScheduler(dataService, clock);
        int updated = scheduler.runOnce();

        assertThat(updated).isEqualTo(1);
        assertThat(todoRepository.findById(overdueSaved.getId())).get()
                .extracting(Todo::getStatus)
                .isEqualTo(TodoStatus.PAST_DUE);
        assertThat(todoRepository.findById(futureSaved.getId())).get()
                .extracting(Todo::getStatus)
                .isEqualTo(TodoStatus.NOT_DONE);
        assertThat(todoRepository.findById(doneSaved.getId())).get()
                .extracting(Todo::getStatus)
                .isEqualTo(TodoStatus.DONE);
    }

    @TestConfiguration
    static class ClockTestConfig {
        @Bean("testClock")
        @Primary
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-03-01T10:00:00Z"), ZoneOffset.UTC);
        }
    }
}
