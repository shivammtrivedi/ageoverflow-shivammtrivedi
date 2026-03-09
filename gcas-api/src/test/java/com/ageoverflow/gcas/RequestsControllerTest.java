package com.ageoverflow.gcas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RequestsControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private static final String INVALID_CUSTOMER_ID = "MillieWasHere";
    private static final String VALID_CUSTOMER_ID = UUID.randomUUID().toString();

    // --- Validation: GET /analysis/{customer_id}/requests ---

    @Test
    void listRequestsInvalidLimitZeroReturns400() {
        var response = restTemplate.getForEntity(
                "/api/v1/analysis/" + VALID_CUSTOMER_ID + "/requests?limit=0", String.class);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void listRequestsInvalidLimitTooHighReturns400() {
        var response = restTemplate.getForEntity(
                "/api/v1/analysis/" + VALID_CUSTOMER_ID + "/requests?limit=2000", String.class);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void listRequestsNegativeOffsetReturns400() {
        var response = restTemplate.getForEntity(
                "/api/v1/analysis/" + VALID_CUSTOMER_ID + "/requests?offset=-1", String.class);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void listRequestsInvalidStartDateReturns400() {
        var response = restTemplate.getForEntity(
                "/api/v1/analysis/" + VALID_CUSTOMER_ID + "/requests?start=not_a_time", String.class);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void listRequestsInvalidEndDateReturns400() {
        var response = restTemplate.getForEntity(
                "/api/v1/analysis/" + VALID_CUSTOMER_ID + "/requests?end=not_a_time", String.class);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void listRequestsInvalidUserIdReturns400() {
        var response = restTemplate.getForEntity(
                "/api/v1/analysis/" + VALID_CUSTOMER_ID + "/requests?user_id=billy", String.class);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void listRequestsInvalidStatusReturns400() {
        var response = restTemplate.getForEntity(
                "/api/v1/analysis/" + VALID_CUSTOMER_ID + "/requests?status=sleepy", String.class);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void listRequestsInvalidGenerationReturns400() {
        var response = restTemplate.getForEntity(
                "/api/v1/analysis/" + VALID_CUSTOMER_ID + "/requests?generation=zillennial", String.class);
        assertEquals(400, response.getStatusCode().value());
    }

    // --- Validation: POST ---

    @Test
    void createRequestWithEmptyPhotosReturns400() {
        String customer = UUID.randomUUID().toString();
        String user = UUID.randomUUID().toString();
        ResponseEntity<String> response = createRequest(customer, user, List.of());
        assertEquals(400, response.getStatusCode().value());
    }

    // --- Pagination: GET /analysis/{customer_id}/requests ---

    @Test
    void listRequestsDefaultLimitIs100() {
        String customer = UUID.randomUUID().toString();
        String user = UUID.randomUUID().toString();

        for (int i = 0; i < 102; i++) {
            createRequest(customer, user, List.of(PhotoHelper.defaultPhoto()));
        }

        var response = restTemplate.getForEntity(
                "/api/v1/analysis/" + customer + "/requests", List.class);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(100, response.getBody().size());
    }

    @Test
    void listRequestsCustomLimitAndOffset() {
        String customer = UUID.randomUUID().toString();
        String user = UUID.randomUUID().toString();

        for (int i = 0; i < 20; i++) {
            createRequest(customer, user, List.of(PhotoHelper.defaultPhoto()));
        }

        // limit=5 should return 5 results
        var page1 = restTemplate.getForEntity(
                "/api/v1/analysis/" + customer + "/requests?limit=5", List.class);
        assertEquals(200, page1.getStatusCode().value());
        assertEquals(5, page1.getBody().size());

        // The item at index 1 in page 1 should match index 0 at offset=1
        String secondId = (String) ((Map<?, ?>) page1.getBody().get(1)).get("id");

        var offset1 = restTemplate.getForEntity(
                "/api/v1/analysis/" + customer + "/requests?limit=1&offset=1", List.class);
        assertEquals(200, offset1.getStatusCode().value());
        assertEquals(1, offset1.getBody().size());
        assertEquals(secondId, ((Map<?, ?>) offset1.getBody().get(0)).get("id"));
    }

    // --- Helper ---

    private ResponseEntity<String> createRequest(String customerId, String userId, List<String> photos) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        Map<String, Object> body = Map.of(
                "user_id", userId,
                "urgent", false,
                "photos", photos
        );

        return restTemplate.postForEntity(
                "/api/v1/analysis/" + customerId + "/requests",
                new HttpEntity<>(body, headers),
                String.class
        );
    }
}
