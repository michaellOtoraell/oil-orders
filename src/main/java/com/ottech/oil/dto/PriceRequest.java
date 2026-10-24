package com.ottech.oil.dto;

import lombok.Data;

@Data
public class PriceRequest {
    private long buyPricePerDumu;
    private long extraCostPerDumu;
    private long sellPricePerLitre;
}
