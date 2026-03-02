package com.demo.todo.repository;

import com.demo.todo.model.Todo;
import com.demo.todo.model.TodoStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository layer for todo persistence operations.
 * <p>
 * Provides CRUD access plus custom queries for overdue synchronization and
 * sorted retrieval.
 */
public interface TodoRepository extends JpaRepository<Todo, Long> {

    /**
     * Transitions all overdue {@code NOT_DONE} records to {@code PAST_DUE}.
     *
     * @param now timestamp used as overdue cutoff
     * @return number of updated rows
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE todos
            SET status = 'PAST_DUE'
            WHERE status = 'NOT_DONE'
              AND due_at < :now
            """, nativeQuery = true)
    int markOverdueAsPastDue(@Param("now") Instant now);

    /**
     * Transitions one overdue {@code NOT_DONE} record to {@code PAST_DUE}.
     *
     * @param id target todo identifier
     * @param now timestamp used as overdue cutoff
     * @return number of updated rows
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE todos
            SET status = 'PAST_DUE'
            WHERE id = :id
              AND status = 'NOT_DONE'
              AND due_at < :now
            """, nativeQuery = true)
    int markOverdueAsPastDueById(@Param("id") Long id, @Param("now") Instant now);

    /**
     * Finds all todos sorted by creation time in ascending order.
     *
     * @return sorted list of all todos
     */
    List<Todo> findAllByOrderByCreatedAtAsc();

    /**
     * Finds todos by status sorted by creation time in ascending order.
     *
     * @param status status filter
     * @return sorted list for the requested status
     */
    List<Todo> findByStatusOrderByCreatedAtAsc(TodoStatus status);
}
