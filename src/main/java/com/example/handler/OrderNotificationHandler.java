package com.example.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.example.model.NotificationResult;
import com.example.model.OrderEvent;
import com.example.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * AWS Lambda Entry Point — triggered by SQS.
 *
 * Think of this like a Spring @RestController, but instead of HTTP requests,
 * it receives SQS messages. One Lambda invocation can process multiple messages (batch).
 *
 * Handler string for AWS Console: com.example.handler.OrderNotificationHandler::handleRequest
 */
public class OrderNotificationHandler implements RequestHandler<SQSEvent, String> {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationHandler.class);

    // ✅ Static fields — initialized once per container, reused across warm starts
    //    Same idea as Spring singleton beans!
    private static final ObjectMapper       objectMapper        = buildObjectMapper();
    private static final NotificationService notificationService = new NotificationService();

    /**
     * Lambda entry point — called for every SQS batch.
     *
     * @param sqsEvent  Contains 1–10 SQS messages (based on batch size config)
     * @param context   Lambda runtime info (function name, remaining time, logger)
     * @return Summary string (logged by CloudWatch)
     */
    @Override
    public String handleRequest(SQSEvent sqsEvent, Context context) {

        log.info("Lambda invoked | function={} | messages={}",
                context.getFunctionName(),
                sqsEvent.getRecords().size());

        List<String> successes = new ArrayList<>();
        List<String> failures  = new ArrayList<>();

        for (SQSEvent.SQSMessage message : sqsEvent.getRecords()) {
            String messageId = message.getMessageId();
            try {
                // ── 1. Deserialize JSON → OrderEvent ──────────────────────────
                //    Same as @RequestBody deserialization in Spring
                OrderEvent order = objectMapper.readValue(message.getBody(), OrderEvent.class);
                log.info("Parsed order | messageId={} | orderId={}", messageId, order.getOrderId());

                // ── 2. Send notifications ─────────────────────────────────────
                NotificationResult result = notificationService.notify(order);

                if (result.isFullySuccessful()) {
                    successes.add(order.getOrderId());
                } else {
                    // Partial success — still counted as processed (no SQS retry)
                    log.warn("Partial success for orderId={}", order.getOrderId());
                    successes.add(order.getOrderId() + "(partial)");
                }

            } catch (Exception e) {
                // ⚠️ Throwing here causes SQS to RETRY the message
                //    Only throw for transient errors (network timeouts etc.)
                log.error("Fatal error processing messageId={} — {}", messageId, e.getMessage(), e);
                failures.add(messageId);

                // Re-throw to trigger SQS retry + Dead Letter Queue after max retries
                throw new RuntimeException("Failed to process SQS message: " + messageId, e);
            }
        }

        String summary = String.format("Processed: %d success, %d failed. Orders: %s",
                successes.size(), failures.size(), successes);
        log.info(summary);
        return summary;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static ObjectMapper buildObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }
}
