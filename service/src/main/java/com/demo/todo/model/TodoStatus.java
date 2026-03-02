package com.demo.todo.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enum of supported todo lifecycle states in domain and persistence layers.
 */
@Schema(description = "Todo lifecycle state")
public enum TodoStatus {
    NOT_DONE,
    DONE,
    PAST_DUE
}
