package com.ottech.oil.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OrderResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String customerLocation;
    private int quantity;
    private String status;
    private LocalDateTime createdAt;
    private int rewardsAvailable;
}
