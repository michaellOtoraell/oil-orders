package com.ottech.oil.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "price_settings")
public class PriceSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private long buyPricePerDumu;
    private long extraCostPerDumu;
    private long sellPricePerLitre;
    private LocalDateTime createdAt;
}
