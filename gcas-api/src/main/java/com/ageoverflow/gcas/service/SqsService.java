package com.ageoverflow.gcas.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.UUID;

/**
 * Publishes analysis request IDs to the appropriate SQS queue.
 * Urgent requests go to the high-priority queue; all others go to the standard queue.
 * The worker service polls both queues and calls the engine for each message.
 */
@Service
public class SqsService {

    private final SqsClient sqsClient;
    private final String urgentQueueUrl;
    private final String standardQueueUrl;

    public SqsService(SqsClient sqsClient,
                      @Value("${aws.sqs.urgent-queue-url}") String urgentQueueUrl,
                      @Value("${aws.sqs.standard-queue-url}") String standardQueueUrl) {
        this.sqsClient = sqsClient;
        this.urgentQueueUrl = urgentQueueUrl;
        this.standardQueueUrl = standardQueueUrl;
    }

    /**
     * Sends the request ID to the urgent or standard SQS queue.
     * The message body is the plain UUID string; the worker parses it back to a UUID.
     */
    public void enqueue(UUID requestId, boolean urgent) {
        String queueUrl = urgent ? urgentQueueUrl : standardQueueUrl;
        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(requestId.toString())
                .build());
    }
}
