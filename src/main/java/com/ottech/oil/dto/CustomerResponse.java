package com.ottech.oil.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CustomerResponse {
    private Long id;
    private String name;
    private String phone;
    private String location;
    private String token;
    private LocalDateTime createdAt;
    private int totalDumu;
    private int progress;
    private int rewardsAvailable;
}
