package com.ottech.oil.dto;

import lombok.Data;

@Data
public class PeriodSummary {
    private String label;
    private long dumu;
    private long revenue;
    private long cost;
    private long rewardsCost;
    private long profit;
}
