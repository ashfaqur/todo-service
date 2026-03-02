package com.demo.todo.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata configuration for generated API documentation.
 * <p>
 * Defines top-level API information rendered in Swagger UI.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Todo Service API",
                version = "1.0.0",
                description = "REST API for managing todo items with due dates, status transitions, and overdue enforcement rules.",
                contact = @Contact(name = "Todo Service Team")
        )
)
public class OpenApiConfig {
}
