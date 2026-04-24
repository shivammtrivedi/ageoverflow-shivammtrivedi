package com.ageoverflow.gcas.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;
import java.util.UUID;

/**
 * SQS polling loop — only active when {@code GCAS_MODE=worker}.
 * Polls the urgent queue first (no wait), then the standard queue (1 s long-poll).
 * Each message body is a plain UUID string written by {@link SqsService#enqueue}.
 * After the engine finishes, the message is deleted so it is not reprocessed.
 */
@Service
@ConditionalOnProperty(name = "gcas.mode", havingValue = "worker")
public class SqsWorkerService {

    private final SqsClient sqsClient;
    private final RequestProcessorService requestProcessorService;
    private final String urgentQueueUrl;
    private final String standardQueueUrl;

    public SqsWorkerService(SqsClient sqsClient,
                            RequestProcessorService requestProcessorService,
                            @Value("${aws.sqs.urgent-queue-url}") String urgentQueueUrl,
                            @Value("${aws.sqs.standard-queue-url}") String standardQueueUrl) {
        this.sqsClient = sqsClient;
        this.requestProcessorService = requestProcessorService;
        this.urgentQueueUrl = urgentQueueUrl;
        this.standardQueueUrl = standardQueueUrl;
    }

    /**
     * Polls urgent queue first (no wait), then standard queue (1 s wait).
     * Runs every 100 ms so the worker stays responsive without busy-spinning.
     */
    @Scheduled(fixedDelay = 100)
    public void poll() {
        // Urgent queue — no wait so we can immediately fall through to standard if empty
        if (pollQueue(urgentQueueUrl, 0)) {
            return;
        }
        // Standard queue — 1 s long-poll to reduce empty receives
        pollQueue(standardQueueUrl, 1);
    }

    /**
     * Receives up to one message from the given queue, processes it, then deletes it.
     *
     * @param queueUrl        the SQS queue to poll
     * @param waitTimeSeconds 0 for short-poll (urgent), 1 for light long-poll (standard)
     * @return true if a message was processed, false if the queue was empty
     */
    private boolean pollQueue(String queueUrl, int waitTimeSeconds) {
        List<Message> messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(1)
                .waitTimeSeconds(waitTimeSeconds)
                .build()).messages();

        if (messages.isEmpty()) {
            return false;
        }

        Message message = messages.get(0);
        UUID requestId = UUID.fromString(message.body());

        requestProcessorService.processRequest(requestId);

        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(message.receiptHandle())
                .build());

        return true;
    }
}
