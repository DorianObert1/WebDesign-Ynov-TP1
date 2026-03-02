package com.ynov.tp1.model;

import java.util.List;

public class OrderRequest {

    private List<String> productIds;
    private String customerId;

    public OrderRequest() {}

    public OrderRequest(List<String> productIds, String customerId) {
        this.productIds = productIds;
        this.customerId = customerId;
    }

    public List<String> getProductIds() { return productIds; }
    public void setProductIds(List<String> productIds) { this.productIds = productIds; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    @Override
    public String toString() {
        return "OrderRequest{productIds=" + productIds + ", customerId='" + customerId + "'}";
    }
}
