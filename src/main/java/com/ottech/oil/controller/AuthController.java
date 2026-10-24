package com.ottech.oil.controller;

import com.ottech.oil.dto.LoginRequest;
import com.ottech.oil.dto.LoginResponse;
import com.ottech.oil.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest r) {
        return authService.login(r);
    }
}
