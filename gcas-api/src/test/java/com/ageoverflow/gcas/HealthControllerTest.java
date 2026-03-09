package com.ageoverflow.gcas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void healthReturns200() {
        var response = restTemplate.getForEntity("/api/v1/health", String.class);
        assertEquals(200, response.getStatusCode().value());
    }
}
