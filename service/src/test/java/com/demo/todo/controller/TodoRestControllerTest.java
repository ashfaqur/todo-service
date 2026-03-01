package com.demo.todo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.demo.todo.dto.TodoResponse;
import com.demo.todo.dto.TodosListMeta;
import com.demo.todo.dto.TodosListResponse;
import com.demo.todo.exception.GlobalExceptionHandler;
import com.demo.todo.exception.TodoNotFoundException;
import com.demo.todo.model.TodoStatus;
import com.demo.todo.service.TodoService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;

@WebMvcTest(TodoRestController.class)
@Import(GlobalExceptionHandler.class)
class TodoRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TodoService todoService;

    @Test
    void createTodoReturns201() throws Exception {
        TodoResponse response = new TodoResponse(
                1L,
                "Pay rent",
                TodoStatus.NOT_DONE,
                Instant.parse("2026-03-01T10:00:00Z"),
                Instant.parse("2026-03-01T11:00:00Z"),
                null
        );

        when(todoService.createTodo(any())).thenReturn(response);

        mockMvc.perform(post("/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Pay rent",
                                  "dueAt": "2026-03-01T11:00:00Z"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/todos/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("Pay rent"))
                .andExpect(jsonPath("$.status").value("NOT_DONE"));
    }

    @Test
    void createTodoReturns400ForInvalidInput() throws Exception {
        mockMvc.perform(post("/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.path").value("/todos"))
                .andExpect(jsonPath("$.timestamp").value("2026-03-01T10:00:00Z"));
    }

    @Test
    void getTodoReturns404WhenIdNotFound() throws Exception {
        when(todoService.getTodoById(99L)).thenThrow(new TodoNotFoundException(99L));

        mockMvc.perform(get("/todos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("TODO_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Todo not found for id: 99"))
                .andExpect(jsonPath("$.path").value("/todos/99"))
                .andExpect(jsonPath("$.timestamp").value("2026-03-01T10:00:00Z"));
    }

    @Test
    void listTodosDefaultReturnsMetaAndItems() throws Exception {
        TodoResponse item = new TodoResponse(
                1L,
                "Task one",
                TodoStatus.NOT_DONE,
                Instant.parse("2026-03-01T09:00:00Z"),
                Instant.parse("2026-03-01T11:00:00Z"),
                null
        );
        TodosListResponse response = new TodosListResponse(List.of(item), new TodosListMeta(1, false));
        when(todoService.listTodos(false)).thenReturn(response);

        mockMvc.perform(get("/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.meta.count").value(1))
                .andExpect(jsonPath("$.meta.all").value(false));
    }

    @Test
    void listTodosAllTrueReturnsMetaAllTrue() throws Exception {
        TodoResponse item = new TodoResponse(
                2L,
                "Past due task",
                TodoStatus.PAST_DUE,
                Instant.parse("2026-03-01T08:00:00Z"),
                Instant.parse("2026-03-01T09:00:00Z"),
                null
        );
        TodosListResponse response = new TodosListResponse(List.of(item), new TodosListMeta(1, true));
        when(todoService.listTodos(true)).thenReturn(response);

        mockMvc.perform(get("/todos").param("all", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].status").value("PAST_DUE"))
                .andExpect(jsonPath("$.meta.count").value(1))
                .andExpect(jsonPath("$.meta.all").value(true));
    }

    @TestConfiguration
    static class ClockTestConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-03-01T10:00:00Z"), ZoneOffset.UTC);
        }
    }
}
