package com.ottech.oil.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    // QR inaelekeza /c/{token} -> page ya mteja
    @GetMapping("/c/{token}")
    public String customerPage() {
        return "forward:/order.html";
    }
}
