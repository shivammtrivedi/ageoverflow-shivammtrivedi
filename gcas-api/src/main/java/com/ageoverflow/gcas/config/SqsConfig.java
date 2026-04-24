package com.ageoverflow.gcas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * Provides a singleton {@link SqsClient} using the default credential chain.
 * On ECS with a task role, credentials are supplied automatically via the
 * container metadata endpoint — no explicit keys needed.
 */
@Configuration
public class SqsConfig {

    @Bean
    public SqsClient sqsClient() {
        return SqsClient.builder()
                .region(Region.US_EAST_1)
                .build();
    }
}
