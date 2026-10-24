package com.ottech.oil.controller;

import com.ottech.oil.dto.CustomerCreateRequest;
import com.ottech.oil.dto.CustomerResponse;
import com.ottech.oil.dto.OrderResponse;
import com.ottech.oil.dto.StatsResponse;
import com.ottech.oil.entity.Customer;
import com.ottech.oil.service.CustomerService;
import com.ottech.oil.service.OrderService;
import com.ottech.oil.service.QrService;
import com.ottech.oil.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private CustomerService customerService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private QrService qrService;
    @Autowired
    private StatsService statsService;

    @GetMapping("/customers")
    public List<CustomerResponse> listCustomers() {
        return customerService.listAll();
    }

    @PostMapping("/customers")
    public CustomerResponse addCustomer(@RequestBody CustomerCreateRequest r) {
        return customerService.create(r);
    }

    @PostMapping("/customers/{id}/redeem")
    public ResponseEntity<CustomerResponse> redeem(@PathVariable Long id) {
        return customerService.redeem(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customers/{id}/qr")
    public ResponseEntity<byte[]> qr(@PathVariable Long id) throws Exception {
        Customer c = customerService.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(qrService.generatePng(c));
    }

    @GetMapping("/stats")
    public StatsResponse stats() {
        return statsService.stats();
    }

    @GetMapping("/orders")
    public List<OrderResponse> listOrders() {
        return orderService.listAll();
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<OrderResponse> setStatus(@PathVariable Long id, @RequestParam String value) {
        return orderService.updateStatus(id, value)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
