package com.ottech.oil.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "orders")
public class CustomerOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String customerLocation;
    private int quantity;
    private String status; // NEW, CONFIRMED, DELIVERED, CANCELLED
    private LocalDateTime createdAt;

    // bei za wakati wa kupeleka (kwa dumu 1), zinawekwa oda ikiwa DELIVERED
    private Long unitCost;
    private Long unitPrice;
    private LocalDateTime deliveredAt;
}
