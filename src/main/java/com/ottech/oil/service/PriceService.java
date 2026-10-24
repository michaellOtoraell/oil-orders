package com.ottech.oil.service;

import com.ottech.oil.dto.PriceRequest;
import com.ottech.oil.dto.PriceResponse;
import com.ottech.oil.entity.PriceSetting;
import com.ottech.oil.exception.BadRequestException;
import com.ottech.oil.repository.PriceSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PriceService {

    public static final int LITRES_PER_DUMU = 20;
    private static final long MAX_PRICE = 100_000_000L;

    @Autowired
    private PriceSettingRepository prices;

    public PriceSetting current() {
        return prices.findFirstByOrderByIdDesc()
                .orElseThrow(() -> new BadRequestException("Bei hazijawekwa bado"));
    }

    public PriceResponse currentResponse() {
        return toResponse(current());
    }

    public List<PriceResponse> history() {
        return prices.findTop20ByOrderByIdDesc().stream().map(this::toResponse).toList();
    }

    // Kila mabadiliko ni safu mpya, historia ya bei inabaki
    public PriceResponse update(PriceRequest r) {
        if (r.getBuyPricePerDumu() <= 0 || r.getBuyPricePerDumu() > MAX_PRICE) {
            throw new BadRequestException("Bei ya kununua si sahihi");
        }
        if (r.getSellPricePerLitre() <= 0 || r.getSellPricePerLitre() > MAX_PRICE) {
            throw new BadRequestException("Bei ya kuuza si sahihi");
        }
        if (r.getExtraCostPerDumu() < 0 || r.getExtraCostPerDumu() > MAX_PRICE) {
            throw new BadRequestException("Gharama nyingine si sahihi");
        }
        PriceSetting p = new PriceSetting();
        p.setBuyPricePerDumu(r.getBuyPricePerDumu());
        p.setExtraCostPerDumu(r.getExtraCostPerDumu());
        p.setSellPricePerLitre(r.getSellPricePerLitre());
        p.setCreatedAt(LocalDateTime.now());
        return toResponse(prices.save(p));
    }

    public PriceResponse toResponse(PriceSetting p) {
        PriceResponse r = new PriceResponse();
        r.setId(p.getId());
        r.setBuyPricePerDumu(p.getBuyPricePerDumu());
        r.setExtraCostPerDumu(p.getExtraCostPerDumu());
        r.setSellPricePerLitre(p.getSellPricePerLitre());
        r.setCostPerDumu(p.getBuyPricePerDumu() + p.getExtraCostPerDumu());
        r.setSellPricePerDumu(p.getSellPricePerLitre() * LITRES_PER_DUMU);
        r.setProfitPerDumu(r.getSellPricePerDumu() - r.getCostPerDumu());
        r.setCreatedAt(p.getCreatedAt());
        return r;
    }
}
