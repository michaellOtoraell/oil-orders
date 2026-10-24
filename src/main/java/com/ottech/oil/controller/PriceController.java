package com.ottech.oil.controller;

import com.ottech.oil.dto.PriceRequest;
import com.ottech.oil.dto.PriceResponse;
import com.ottech.oil.service.PriceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/prices")
public class PriceController {

    @Autowired
    private PriceService priceService;

    // ya kwanza kwenye orodha ndiyo bei ya sasa
    @GetMapping
    public List<PriceResponse> history() {
        return priceService.history();
    }

    @PostMapping
    public PriceResponse update(@RequestBody PriceRequest r) {
        return priceService.update(r);
    }
}
