package com.example.service;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.example.model.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Sends SMS notifications via AWS SNS (SDK v1).
 * SDK v1 is lighter (~0.8MB) vs SDK v2 (~1.2MB) for Lambda.
 */
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    // ✅ Static client — reused across warm Lambda invocations
    private static final AmazonSNS snsClient =
            AmazonSNSClientBuilder.standard()
                    .withRegion(Regions.AP_SOUTH_2)
                    .build();

    private static final String SENDER_ID =
            System.getenv().getOrDefault("SMS_SENDER_ID", "MYSTORE");

    /**
     * Sends SMS to customer phone number.
     * @return SNS message ID
     */
    public String sendOrderSms(OrderEvent order) {
        String phoneNumber = order.getCustomerPhone();
        String message     = buildSmsMessage(order);

        log.info("Sending SMS to: {}", maskPhone(phoneNumber));

        PublishRequest request = new PublishRequest()
                .withPhoneNumber(phoneNumber)
                .withMessage(message)
                .withMessageAttributes(Map.of(
                        // ✅ Transactional = high priority, bypasses DND
                        "AWS.SNS.SMS.SMSType", new MessageAttributeValue()
                                .withDataType("String")
                                .withStringValue("Transactional"),
                        // ✅ Sender ID shown on customer's phone
                        "AWS.SNS.SMS.SenderID", new MessageAttributeValue()
                                .withDataType("String")
                                .withStringValue(SENDER_ID)
                ));

        PublishResult result = snsClient.publish(request);
        log.info("SMS sent. MessageId: {}", result.getMessageId());
        return result.getMessageId();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String buildSmsMessage(OrderEvent order) {
        // Keep under 160 chars — avoids split SMS extra charges
        return String.format(
            "Hi %s! Order #%s confirmed. Amount: Rs.%.0f. Est. delivery: %s. Track: mystore.com - MyStore",
            order.getCustomerName().split(" ")[0],
            order.getOrderId(),
            order.getTotalAmount(),
            order.getEstimatedDelivery()
        );
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return "***";
        return phone.substring(0, 3) + "******" + phone.substring(phone.length() - 4);
    }
}
