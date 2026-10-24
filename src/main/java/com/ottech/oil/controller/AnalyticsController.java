package com.ottech.oil.controller;

import com.ottech.oil.dto.AnalyticsResponse;
import com.ottech.oil.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    // period = daily | weekly | monthly
    @GetMapping
    public AnalyticsResponse analytics(@RequestParam(defaultValue = "daily") String period) {
        return analyticsService.build(period);
    }
}
