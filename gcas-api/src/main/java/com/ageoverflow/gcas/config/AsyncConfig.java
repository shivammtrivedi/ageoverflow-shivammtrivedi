package com.ageoverflow.gcas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Replaces Spring's default SimpleAsyncTaskExecutor (unbounded, spawns a new thread per task)
 * with a bounded pool so a burst of POST requests cannot exhaust DB connections or JVM memory.
 *
 * Sizing rationale:
 *  - corePoolSize=4:    always-ready threads; matches the number of engine processes we want
 *                       running concurrently without contention.
 *  - maxPoolSize=16:    burst headroom; kept well below the HikariCP pool size so we never
 *                       have more threads competing for connections than there are connections.
 *  - queueCapacity=500: jobs queue here before new threads beyond core are spawned, providing
 *                       backpressure without dropping requests.
 *  - CallerRunsPolicy:  if both the queue and the pool are full, the HTTP request thread
 *                       processes the job itself — natural backpressure rather than rejection.
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "engineExecutor")
    public TaskExecutor engineExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("engine-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
