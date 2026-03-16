package com.example;

import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final List<Order> orders = Arrays.asList(
        new Order(1L, 1L, "Laptop", 5999.99),
        new Order(2L, 2L, "Phone", 2999.00),
        new Order(3L, 1L, "Mouse", 99.00)
    );

    @GetMapping
    public List<Order> getAllOrders() {
        return orders;
    }

    @GetMapping("/{id}")
    public Order getOrder(@PathVariable Long id) {
        return orders.stream()
            .filter(o -> o.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    @GetMapping("/user/{userId}")
    public List<Order> getOrdersByUser(@PathVariable Long userId) {
        return orders.stream()
            .filter(o -> o.getUserId().equals(userId))
            .toList();
    }
}
