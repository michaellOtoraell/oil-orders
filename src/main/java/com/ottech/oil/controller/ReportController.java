package com.ottech.oil.controller;

import com.ottech.oil.dto.ReportResponse;
import com.ottech.oil.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/report")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping
    public ReportResponse report() {
        return reportService.report();
    }
}
