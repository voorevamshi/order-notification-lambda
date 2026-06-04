package com.example.model;

public class NotificationResult {

    private String orderId;
    private boolean emailSent;
    private boolean smsSent;
    private String emailMessageId;
    private String smsMessageId;
    private String errorMessage;

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getOrderId()              { return orderId; }
    public void setOrderId(String v)        { this.orderId = v; }

    public boolean isEmailSent()            { return emailSent; }
    public void setEmailSent(boolean v)     { this.emailSent = v; }

    public boolean isSmsSent()              { return smsSent; }
    public void setSmsSent(boolean v)       { this.smsSent = v; }

    public String getEmailMessageId()       { return emailMessageId; }
    public void setEmailMessageId(String v) { this.emailMessageId = v; }

    public String getSmsMessageId()         { return smsMessageId; }
    public void setSmsMessageId(String v)   { this.smsMessageId = v; }

    public String getErrorMessage()         { return errorMessage; }
    public void setErrorMessage(String v)   { this.errorMessage = v; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public boolean isFullySuccessful() {
        return emailSent && smsSent;
    }

    public String getSummary() {
        return String.format(
            "Order [%s] → Email: %s | SMS: %s",
            orderId,
            emailSent ? "✅ Sent (" + emailMessageId + ")" : "❌ Failed",
            smsSent   ? "✅ Sent (" + smsMessageId   + ")" : "❌ Failed"
        );
    }
}
