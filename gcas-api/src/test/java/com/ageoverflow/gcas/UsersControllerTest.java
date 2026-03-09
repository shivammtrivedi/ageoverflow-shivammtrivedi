package com.ageoverflow.gcas;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UsersControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private static final String VALID_CUSTOMER_ID = UUID.randomUUID().toString();

    // --- Validation: GET /analysis/{customer_id}/users ---

    @Test
    void listUsersInvalidLimitZeroReturns400() {
        var response = restTemplate.getForEntity(
                "/api/v1/analysis/" + VALID_CUSTOMER_ID + "/users?limit=0", String.class);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void listUsersInvalidLimitTooHighReturns400() {
        var response = restTemplate.getForEntity(
                "/api/v1/analysis/" + VALID_CUSTOMER_ID + "/users?limit=2000", String.class);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void listUsersNegativeOffsetReturns400() {
        var response = restTemplate.getForEntity(
                "/api/v1/analysis/" + VALID_CUSTOMER_ID + "/users?offset=-1", String.class);
        assertEquals(400, response.getStatusCode().value());
    }

    // --- Pagination: GET /analysis/{customer_id}/users ---

    @Test
    void listUsersDefaultLimitIs100() {
        String customer = UUID.randomUUID().toString();

        // Each request uses a unique user so we get 102 distinct users
        for (int i = 0; i < 102; i++) {
            createRequest(customer, UUID.randomUUID().toString());
        }

        var response = restTemplate.getForEntity(
                "/api/v1/analysis/" + customer + "/users", List.class);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(100, response.getBody().size());
    }

    @Test
    void listUsersCustomLimitAndOffset() {
        String customer = UUID.randomUUID().toString();

        for (int i = 0; i < 20; i++) {
            createRequest(customer, UUID.randomUUID().toString());
        }

        // limit=5 should return 5 users
        var page1 = restTemplate.getForEntity(
                "/api/v1/analysis/" + customer + "/users?limit=5", List.class);
        assertEquals(200, page1.getStatusCode().value());
        assertEquals(5, page1.getBody().size());

        // The user at index 1 should match the user at offset=1
        String secondId = (String) ((Map<?, ?>) page1.getBody().get(1)).get("id");

        var offset1 = restTemplate.getForEntity(
                "/api/v1/analysis/" + customer + "/users?limit=1&offset=1", List.class);
        assertEquals(200, offset1.getStatusCode().value());
        assertEquals(1, offset1.getBody().size());
        assertEquals(secondId, ((Map<?, ?>) offset1.getBody().get(0)).get("id"));
    }

    // --- Helper ---

    private void createRequest(String customerId, String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        Map<String, Object> body = Map.of(
                "user_id", userId,
                "urgent", false,
                "photos", List.of(PhotoHelper.defaultPhoto())
        );

        restTemplate.postForEntity(
                "/api/v1/analysis/" + customerId + "/requests",
                new HttpEntity<>(body, headers),
                String.class
        );
    }
}
