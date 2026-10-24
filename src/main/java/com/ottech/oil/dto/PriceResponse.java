package com.ottech.oil.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PriceResponse {
    private Long id;
    private long buyPricePerDumu;
    private long extraCostPerDumu;
    private long sellPricePerLitre;
    private long costPerDumu;
    private long sellPricePerDumu;
    private long profitPerDumu;
    private LocalDateTime createdAt;
}
