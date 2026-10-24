package com.ottech.oil.controller;

import com.ottech.oil.dto.MessageResponse;
import com.ottech.oil.dto.OrderRequest;
import com.ottech.oil.entity.Customer;
import com.ottech.oil.service.CustomerService;
import com.ottech.oil.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/c/{token}")
public class PublicController {

    @Autowired
    private CustomerService customerService;
    @Autowired
    private OrderService orderService;

    @GetMapping
    public ResponseEntity<?> info(@PathVariable String token) {
        Customer c = customerService.findByToken(token).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(customerService.info(c));
    }

    @GetMapping("/orders")
    public ResponseEntity<?> orders(@PathVariable String token) {
        Customer c = customerService.findByToken(token).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(orderService.history(c));
    }

    @PostMapping
    public ResponseEntity<?> order(@PathVariable String token, @RequestBody OrderRequest r) {
        Customer c = customerService.findByToken(token).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        orderService.place(c, r.getQuantity());
        return ResponseEntity.ok(new MessageResponse("Asante! Tutakupigia simu kuthibitisha oda yako."));
    }
}
