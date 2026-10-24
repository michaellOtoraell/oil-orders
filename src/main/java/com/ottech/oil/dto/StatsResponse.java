package com.ottech.oil.dto;

import lombok.Data;

@Data
public class StatsResponse {
    private long totalOrders;
    private long totalDumu;
    private long newOrders;
    private long deliveredOrders;
}
