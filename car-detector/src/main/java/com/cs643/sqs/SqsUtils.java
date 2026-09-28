package com.cs643.sqs;
// Import Statements
import com.cs643.util.Retry;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;
import java.util.Optional;

// SQS helper functions: send, receive with long polling, and delete messages
public final class SqsUtils {
    private SqsUtils(){}

    // Based On Parameters Provided, Send SQS Message to Queue with Retry
    public static void send(SqsClient sqs, String queueUrl, String body) {
        SendMessageRequest req = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(body)
                .build();
        Retry.withBackoff("sqs-send", 4, () -> sqs.sendMessage(req));
    }

    // Long-poll SQS Queue (Up to 20s) to fetch one message with retries
    // If there is no message return Optional.empty, if there is a message, return the first message received (Optional.of(message))
    public static Optional<Message> receiveOne(SqsClient sqs, String queueUrl) {
        ReceiveMessageRequest req = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(1)
                .waitTimeSeconds(20) // long poll to reduce empty receives
                .build();
        ReceiveMessageResponse resp = Retry.withBackoff("sqs-receive", 4, () -> sqs.receiveMessage(req));
        List<Message> msgs = resp.messages();
        return (msgs == null || msgs.isEmpty()) ? Optional.empty() : Optional.of(msgs.get(0));
    }

    // Delete the processed SQS message by receipt handle with retries.
    // Cleans the queue of already read messages.
    public static void delete(SqsClient sqs, String queueUrl, Message m) {
        DeleteMessageRequest req = DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(m.receiptHandle())
                .build();
        Retry.withBackoff("sqs-delete", 4, () -> {
            sqs.deleteMessage(req);
            return true;
        });
    }
    // Clear SQS Message Queue Completely.
    public static void drain(SqsClient sqs, String queueUrl) {
        while (true) {
            Optional<Message> m = receiveOne(sqs, queueUrl);
            if (m.isEmpty()) return; // stop when queue looks empty
            delete(sqs, queueUrl, m.get());
        }
    }
}
