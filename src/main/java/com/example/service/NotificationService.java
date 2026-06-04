package com.example.service;

import com.example.model.NotificationResult;
import com.example.model.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final EmailService emailService;
    private final SmsService   smsService;

    public NotificationService() {
        this.emailService = new EmailService();
        this.smsService   = new SmsService();
    }

    // Constructor for unit testing (inject mocks)
    public NotificationService(EmailService emailService, SmsService smsService) {
        this.emailService = emailService;
        this.smsService   = smsService;
    }

    public NotificationResult notify(OrderEvent order) {
        log.info("Processing notifications for orderId={}", order.getOrderId());

        NotificationResult result = new NotificationResult();
        result.setOrderId(order.getOrderId());

        // ── Email ─────────────────────────────────────────────────────────────
        try {
            String messageId = emailService.sendOrderConfirmation(order);
            result.setEmailSent(true);
            result.setEmailMessageId(messageId);
            log.info("Email OK — messageId={}", messageId);
        } catch (Exception e) {
            result.setEmailSent(false);
            log.error("Email FAILED for orderId={} — {}", order.getOrderId(), e.getMessage(), e);
        }

        // ── SMS ───────────────────────────────────────────────────────────────
        try {
            if (order.getCustomerPhone() != null && !order.getCustomerPhone().isBlank()) {
                String messageId = smsService.sendOrderSms(order);
                result.setSmsSent(true);
                result.setSmsMessageId(messageId);
                log.info("SMS OK — messageId={}", messageId);
            } else {
                log.warn("No phone number for orderId={} — skipping SMS", order.getOrderId());
                result.setSmsSent(false);
                result.setSmsMessageId("SKIPPED_NO_PHONE");
            }
        } catch (Exception e) {
            result.setSmsSent(false);
            log.error("SMS FAILED for orderId={} — {}", order.getOrderId(), e.getMessage(), e);
        }

        log.info(result.getSummary());
        return result;
    }
}
