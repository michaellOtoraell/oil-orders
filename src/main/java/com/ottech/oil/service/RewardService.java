package com.ottech.oil.service;

import com.ottech.oil.entity.Customer;
import com.ottech.oil.entity.RewardRedemption;
import com.ottech.oil.repository.RewardRedemptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RewardService {

    @Autowired
    private RewardRedemptionRepository redemptions;
    @Autowired
    private PriceService prices;

    // Lita 1 bure inagharimu bei ya kununua ya lita 1 (kwa bei za sasa)
    public RewardRedemption record(Customer c) {
        RewardRedemption r = new RewardRedemption();
        r.setCustomerId(c.getId());
        r.setCost(prices.current().getBuyPricePerDumu() / PriceService.LITRES_PER_DUMU);
        r.setCreatedAt(LocalDateTime.now());
        return redemptions.save(r);
    }
}
