package com.ottech.oil.dto;

import lombok.Data;
import java.util.List;

@Data
public class AnalyticsResponse {
    private String period;
    private int rangeLength;
    // tarehe ya kuanza ya kila kipindi (yyyy-MM-dd)
    private List<String> labels;
    private List<Long> orders;
    private List<Long> dumuOrdered;
    private List<Long> dumuDelivered;
    private List<Long> newCustomers;
    private List<Long> revenue;
    private List<Long> cost;
    private List<Long> profit;
    private AnalyticsTotals current;
    private AnalyticsTotals previous;
    private List<ChartPoint> ordersByStatus;
    private List<ChartPoint> topCustomers;
    private List<ChartPoint> orderSizes;
}
