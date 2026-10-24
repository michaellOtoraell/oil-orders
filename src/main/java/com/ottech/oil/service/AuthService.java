package com.ottech.oil.service;

import com.ottech.oil.dto.LoginRequest;
import com.ottech.oil.dto.LoginResponse;
import com.ottech.oil.exception.BadRequestException;
import com.ottech.oil.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class AuthService {

    @Value("${app.admin.username}")
    private String adminUser;
    @Value("${app.admin.password}")
    private String adminPass;

    @Autowired
    private JwtService jwt;

    public LoginResponse login(LoginRequest r) {
        boolean ok = same(adminUser, r.getUsername()) & same(adminPass, r.getPassword());
        if (!ok) {
            throw new BadRequestException("Jina au nenosiri si sahihi");
        }
        return new LoginResponse(jwt.create(adminUser));
    }

    private boolean same(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
