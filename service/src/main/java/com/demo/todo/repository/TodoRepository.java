package com.demo.todo.repository;

import com.demo.todo.model.Todo;
import com.demo.todo.model.TodoStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE todos
            SET status = 'PAST_DUE'
            WHERE status = 'NOT_DONE'
              AND due_at < :now
            """, nativeQuery = true)
    int markOverdueAsPastDue(@Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE todos
            SET status = 'PAST_DUE'
            WHERE id = :id
              AND status = 'NOT_DONE'
              AND due_at < :now
            """, nativeQuery = true)
    int markOverdueAsPastDueById(@Param("id") Long id, @Param("now") Instant now);

    List<Todo> findAllByOrderByCreatedAtAsc();

    List<Todo> findByStatusOrderByCreatedAtAsc(TodoStatus status);
}
