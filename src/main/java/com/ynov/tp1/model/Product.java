package com.ynov.tp1.model;

import java.math.BigDecimal;

public class Product {

    private String id;
    private String name;
    private BigDecimal price; // > 0
    private Integer stock;   // >= 0
    private String category;

    public Product() {}

    public Product(String id, String name, BigDecimal price, Integer stock, String category) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.category = category;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    @Override
    public String toString() {
        return "Product{id='" + id + "', name='" + name + "', price=" + price +
               ", stock=" + stock + ", category='" + category + "'}";
    }
}
