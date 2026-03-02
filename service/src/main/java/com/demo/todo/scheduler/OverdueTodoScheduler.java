package com.demo.todo.scheduler;

import com.demo.todo.service.DataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * Scheduled background job that transitions overdue todos to {@code PAST_DUE}.
 */
@Component
@ConditionalOnProperty(
        prefix = "todo.overdue-sync.scheduler",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OverdueTodoScheduler {

    private static final Logger log = LoggerFactory.getLogger(OverdueTodoScheduler.class);

    private final DataService dataService;
    private final Clock clock;

    /**
     * Creates the scheduler with service and time dependencies.
     *
     * @param dataService transaction boundary for overdue synchronization
     * @param clock       clock used to compute the current timestamp
     */
    public OverdueTodoScheduler(DataService dataService, Clock clock) {
        this.dataService = dataService;
        this.clock = clock;
    }

    /**
     * Runs the overdue synchronization based on configured fixed delay.
     */
    @Scheduled(fixedDelayString = "${todo.overdue-sync.scheduler.fixed-delay}")
    public void syncOverdueScheduled() {
        runOnce();
    }

    /**
     * Executes one synchronization cycle.
     *
     * @return number of updated records
     */
    public int runOnce() {
        Instant now = Instant.now(clock);
        int updated = dataService.syncOverdue(now);
        log.debug("Overdue todo sync completed at {}. Updated rows: {}", now, updated);
        return updated;
    }
}
