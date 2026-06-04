package com.example.service;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;
import com.example.model.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

/**
 * Sends order confirmation emails via AWS SES (SDK v1).
 * SDK v1 is lighter (~1MB) vs SDK v2 (~2.5MB) for Lambda.
 */
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    // ✅ Static client — reused across warm Lambda invocations
    private static final AmazonSimpleEmailService sesClient =
            AmazonSimpleEmailServiceClientBuilder.standard()
                    .withRegion(Regions.AP_SOUTH_2)
                    .build();

    private static final String SENDER_EMAIL =
            System.getenv().getOrDefault("SENDER_EMAIL", "noreply@mystore.com");

    /**
     * Sends HTML + plain-text order confirmation email.
     * @return SES message ID
     */
    public String sendOrderConfirmation(OrderEvent order) {
        log.info("Sending email to: {}", order.getCustomerEmail());

        SendEmailRequest request = new SendEmailRequest()
                .withSource(SENDER_EMAIL)
                .withDestination(new Destination()
                        .withToAddresses(order.getCustomerEmail()))
                .withMessage(new Message()
                        .withSubject(new Content(buildSubject(order)))
                        .withBody(new Body()
                                .withHtml(new Content().withData(buildHtmlBody(order)).withCharset("UTF-8"))
                                .withText(new Content().withData(buildTextBody(order)).withCharset("UTF-8"))
                        )
                );

        SendEmailResult result = sesClient.sendEmail(request);
        log.info("Email sent. MessageId: {}", result.getMessageId());
        return result.getMessageId();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String buildSubject(OrderEvent order) {
        return "✅ Order Confirmed! #" + order.getOrderId() + " — MyStore";
    }

    private String buildHtmlBody(OrderEvent order) {
        String itemRows = order.getItems().stream()
                .map(item -> String.format(
                        "<tr><td>%s</td><td style='text-align:center'>%d</td><td style='text-align:right'>₹%.2f</td></tr>",
                        item.getProductName(), item.getQuantity(), item.getPrice()))
                .collect(Collectors.joining("\n"));

        return """
            <!DOCTYPE html>
            <html>
            <head>
              <style>
                body { font-family: Arial, sans-serif; color: #333; }
                .header { background: #ff6200; color: white; padding: 20px; text-align: center; }
                .body   { padding: 20px; }
                table   { width: 100%%; border-collapse: collapse; margin: 16px 0; }
                th      { background: #f5f5f5; padding: 10px; text-align: left; border-bottom: 2px solid #ddd; }
                td      { padding: 10px; border-bottom: 1px solid #eee; }
                .total  { font-weight: bold; font-size: 16px; }
                .footer { background: #f5f5f5; padding: 16px; text-align: center; font-size: 12px; color: #888; }
              </style>
            </head>
            <body>
              <div class="header"><h2>🛍️ Order Confirmed!</h2></div>
              <div class="body">
                <p>Dear <strong>%s</strong>,</p>
                <p>Thank you! Your order <strong>#%s</strong> has been placed successfully.</p>
                <table>
                  <thead>
                    <tr><th>Product</th><th style="text-align:center">Qty</th><th style="text-align:right">Price</th></tr>
                  </thead>
                  <tbody>%s</tbody>
                </table>
                <p class="total">Total Amount: ₹%.2f %s</p>
                <p>📦 Estimated Delivery: <strong>%s</strong></p>
                <p>🏠 Delivery Address: %s</p>
              </div>
              <div class="footer">MyStore | support@mystore.com</div>
            </body>
            </html>
            """.formatted(
                order.getCustomerName(), order.getOrderId(), itemRows,
                order.getTotalAmount(), order.getCurrency(),
                order.getEstimatedDelivery(), order.getDeliveryAddress()
        );
    }

    private String buildTextBody(OrderEvent order) {
        String items = order.getItems().stream()
                .map(i -> String.format("  - %s x%d @ ₹%.2f",
                        i.getProductName(), i.getQuantity(), i.getPrice()))
                .collect(Collectors.joining("\n"));

        return String.format("""
            Dear %s,
            Your order #%s is confirmed!
            Items:
            %s
            Total: ₹%.2f %s
            Estimated Delivery: %s
            Address: %s
            Thank you — MyStore
            """,
                order.getCustomerName(), order.getOrderId(), items,
                order.getTotalAmount(), order.getCurrency(),
                order.getEstimatedDelivery(), order.getDeliveryAddress()
        );
    }
}
