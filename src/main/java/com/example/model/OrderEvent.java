package com.example.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderEvent {

    @JsonProperty("orderId")        private String orderId;
    @JsonProperty("customerId")     private String customerId;
    @JsonProperty("customerName")   private String customerName;
    @JsonProperty("customerEmail")  private String customerEmail;
    @JsonProperty("customerPhone")  private String customerPhone;
    @JsonProperty("totalAmount")    private double totalAmount;
    @JsonProperty("currency")       private String currency;
    @JsonProperty("status")         private String status;
    @JsonProperty("items")          private List<OrderItem> items;
    @JsonProperty("deliveryAddress")   private String deliveryAddress;
    @JsonProperty("estimatedDelivery") private String estimatedDelivery;
    @JsonProperty("createdAt")         private String createdAt;

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getOrderId()            { return orderId; }
    public void setOrderId(String v)      { this.orderId = v; }

    public String getCustomerId()         { return customerId; }
    public void setCustomerId(String v)   { this.customerId = v; }

    public String getCustomerName()       { return customerName; }
    public void setCustomerName(String v) { this.customerName = v; }

    public String getCustomerEmail()       { return customerEmail; }
    public void setCustomerEmail(String v) { this.customerEmail = v; }

    public String getCustomerPhone()       { return customerPhone; }
    public void setCustomerPhone(String v) { this.customerPhone = v; }

    public double getTotalAmount()         { return totalAmount; }
    public void setTotalAmount(double v)   { this.totalAmount = v; }

    public String getCurrency()            { return currency; }
    public void setCurrency(String v)      { this.currency = v; }

    public String getStatus()             { return status; }
    public void setStatus(String v)       { this.status = v; }

    public List<OrderItem> getItems()          { return items; }
    public void setItems(List<OrderItem> v)    { this.items = v; }

    public String getDeliveryAddress()         { return deliveryAddress; }
    public void setDeliveryAddress(String v)   { this.deliveryAddress = v; }

    public String getEstimatedDelivery()       { return estimatedDelivery; }
    public void setEstimatedDelivery(String v) { this.estimatedDelivery = v; }

    public String getCreatedAt()           { return createdAt; }
    public void setCreatedAt(String v)     { this.createdAt = v; }

    // ── Inner class ───────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderItem {

        @JsonProperty("productName") private String productName;
        @JsonProperty("quantity")    private int quantity;
        @JsonProperty("price")       private double price;

        public String getProductName()        { return productName; }
        public void setProductName(String v)  { this.productName = v; }

        public int getQuantity()              { return quantity; }
        public void setQuantity(int v)        { this.quantity = v; }

        public double getPrice()              { return price; }
        public void setPrice(double v)        { this.price = v; }
    }
}
