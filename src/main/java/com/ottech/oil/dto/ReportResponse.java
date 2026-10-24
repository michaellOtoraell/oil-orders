package com.ottech.oil.dto;

import lombok.Data;
import java.util.List;

@Data
public class ReportResponse {
    private List<PeriodSummary> periods;
    private PriceResponse price;
    // oda ambazo bado hazijapelekwa (NEW na CONFIRMED), kwa bei za sasa
    private long pendingDumu;
    private long expectedProfit;
}
