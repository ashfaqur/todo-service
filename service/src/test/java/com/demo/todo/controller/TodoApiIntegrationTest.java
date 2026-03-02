package com.demo.todo.controller;

import com.demo.todo.model.Todo;
import com.demo.todo.model.TodoStatus;
import com.demo.todo.repository.TodoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TodoApiIntegrationTest.ClockTestConfig.class)
class TodoApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        todoRepository.deleteAll();
    }

    @Test
    void createThenGetLifecycleReturnsPersistedTodo() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Pay rent",
                                  "dueAt": "2026-03-01T11:00:00Z"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/todos/")))
                .andExpect(jsonPath("$.description").value("Pay rent"))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long id = created.get("id").asLong();

        mockMvc.perform(get("/todos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.description").value("Pay rent"))
                .andExpect(jsonPath("$.status").value("NOT_DONE"));
    }

    @Test
    void getTodoByIdSynchronizesOverdueNotDoneToPastDue() throws Exception {
        Instant now = Instant.parse("2026-03-01T10:00:00Z");
        Todo todo = new Todo();
        todo.setDescription("Overdue task");
        todo.setStatus(TodoStatus.NOT_DONE);
        todo.setCreatedAt(now.minusSeconds(100));
        todo.setDueAt(now.minusSeconds(1));
        todo.setDoneAt(null);
        Todo saved = todoRepository.save(todo);

        mockMvc.perform(get("/todos/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAST_DUE"));

        Todo refreshed = todoRepository.findById(saved.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo(TodoStatus.PAST_DUE);
    }

    @Test
    void patchDescriptionReturnsPastDueImmutableConflictPayload() throws Exception {
        Instant now = Instant.parse("2026-03-01T10:00:00Z");
        Todo todo = new Todo();
        todo.setDescription("Overdue task");
        todo.setStatus(TodoStatus.NOT_DONE);
        todo.setCreatedAt(now.minusSeconds(100));
        todo.setDueAt(now.minusSeconds(1));
        todo.setDoneAt(null);
        Todo saved = todoRepository.save(todo);

        mockMvc.perform(patch("/todos/{id}/description", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Updated"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("PAST_DUE_IMMUTABLE"))
                .andExpect(jsonPath("$.path").value("/todos/%d/description".formatted(saved.getId())));
    }

    @Test
    void markNotDoneReturnsOverdueReopenForbiddenConflictPayload() throws Exception {
        Instant now = Instant.parse("2026-03-01T10:00:00Z");
        Todo todo = new Todo();
        todo.setDescription("Done overdue task");
        todo.setStatus(TodoStatus.DONE);
        todo.setCreatedAt(now.minusSeconds(100));
        todo.setDueAt(now.minusSeconds(1));
        todo.setDoneAt(now.minusSeconds(50));
        Todo saved = todoRepository.save(todo);

        mockMvc.perform(post("/todos/{id}/not-done", saved.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("OVERDUE_REOPEN_FORBIDDEN"))
                .andExpect(jsonPath("$.path").value("/todos/%d/not-done".formatted(saved.getId())));
    }

    @Test
    void invalidAllParamReturnsStandardErrorResponse() throws Exception {
        mockMvc.perform(get("/todos").param("all", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message", containsString("all")))
                .andExpect(jsonPath("$.path").value("/todos"))
                .andExpect(jsonPath("$.timestamp").value("2026-03-01T10:00:00Z"));
    }

    @Test
    void invalidPathIdReturnsStandardErrorResponse() throws Exception {
        mockMvc.perform(get("/todos/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message", containsString("id")))
                .andExpect(jsonPath("$.path").value("/todos/not-a-number"))
                .andExpect(jsonPath("$.timestamp").value("2026-03-01T10:00:00Z"));
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
