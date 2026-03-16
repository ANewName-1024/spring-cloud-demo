package com.example;

public class Order {
    private Long id;
    private Long userId;
    private String product;
    private Double price;

    public Order() {}

    public Order(Long id, Long userId, String product, Double price) {
        this.id = id;
        this.userId = userId;
        this.product = product;
        this.price = price;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = product; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
}
