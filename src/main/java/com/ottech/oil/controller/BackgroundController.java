package com.ottech.oil.controller;

import com.ottech.oil.service.BackgroundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/backgrounds")
public class BackgroundController {

    @Autowired
    private BackgroundService backgroundService;

    @GetMapping
    public List<String> list() {
        return backgroundService.list();
    }
}
