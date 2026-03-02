package com.demo.todo.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for application-wide time dependencies.
 * <p>
 * Exposes a UTC clock bean used by service and error handling layers.
 */
@Configuration
public class TimeConfig {

    /**
     * Provides the default UTC clock for production runtime.
     *
     * @return system UTC clock instance
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
