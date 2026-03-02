package com.ynov.tp1.model;

import java.math.BigDecimal;

public class ProductWithPrice {

    private Product product;
    private BigDecimal originalPrice;
    private Integer discountPercentage; // en %
    private BigDecimal finalPrice;

    public ProductWithPrice() {}

    public ProductWithPrice(Product product, BigDecimal originalPrice,
                            Integer discountPercentage, BigDecimal finalPrice) {
        this.product = product;
        this.originalPrice = originalPrice;
        this.discountPercentage = discountPercentage;
        this.finalPrice = finalPrice;
    }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }

    public Integer getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(Integer discountPercentage) { this.discountPercentage = discountPercentage; }

    public BigDecimal getFinalPrice() { return finalPrice; }
    public void setFinalPrice(BigDecimal finalPrice) { this.finalPrice = finalPrice; }

    @Override
    public String toString() {
        return "ProductWithPrice{product=" + product + ", originalPrice=" + originalPrice +
               ", discountPercentage=" + discountPercentage + "%, finalPrice=" + finalPrice + "}";
    }
}
