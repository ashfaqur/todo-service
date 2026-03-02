package com.demo.todo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the Todo service application.
 * <p>
 * Bootstraps the runtime container and auto-configuration.
 */
@SpringBootApplication
public class TodoApplication {

    /**
     * Starts the Todo service process.
     *
     * @param args command-line arguments passed at startup
     */
    public static void main(String[] args) {
        SpringApplication.run(TodoApplication.class, args);
    }

}
