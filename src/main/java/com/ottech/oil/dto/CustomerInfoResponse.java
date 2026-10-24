package com.ottech.oil.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CustomerInfoResponse {
    private String name;
    private int progress;
    private int target;
    private int rewardsAvailable;
    private int totalDumu;
    private int lastOrderQuantity;
    private LocalDateTime lastOrderAt;
    private String lastOrderStatus;
}
