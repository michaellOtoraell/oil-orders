package com.ottech.oil.dto;

import lombok.Data;

@Data
public class AnalyticsTotals {
    private long orders;
    private long dumuOrdered;
    private long dumuDelivered;
    private long newCustomers;
    private long revenue;
    private long cost;
    private long profit;
    private double avgDumuPerOrder;
    private double deliveryRate;
}
