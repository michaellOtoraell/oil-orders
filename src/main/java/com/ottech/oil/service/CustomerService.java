package com.ottech.oil.service;

import com.ottech.oil.dto.CustomerCreateRequest;
import com.ottech.oil.dto.CustomerInfoResponse;
import com.ottech.oil.dto.CustomerResponse;
import com.ottech.oil.dto.StampSummary;
import com.ottech.oil.entity.Customer;
import com.ottech.oil.exception.BadRequestException;
import com.ottech.oil.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customers;
    @Autowired
    private StampService stamps;
    @Autowired
    private RewardService rewards;
    @Autowired
    private com.ottech.oil.repository.OrderRepository orderRepository;

    public List<CustomerResponse> listAll() {
        return customers.findAll().stream().map(this::toResponse).toList();
    }

    public CustomerResponse create(CustomerCreateRequest r) {
        if (r.getName() == null || r.getName().isBlank()) {
            throw new BadRequestException("Jina linahitajika");
        }
        Customer c = new Customer();
        c.setName(r.getName().trim());
        c.setPhone(r.getPhone());
        c.setLocation(r.getLocation());
        c.setToken(UUID.randomUUID().toString().replace("-", ""));
        c.setCreatedAt(LocalDateTime.now());
        c.setRewardsRedeemed(0);
        return toResponse(customers.save(c));
    }

    public Optional<Customer> findById(Long id) {
        return customers.findById(id);
    }

    public Optional<Customer> findByToken(String token) {
        return customers.findByToken(token);
    }

    // Mteja amepewa lita 1 bure -> punguza zawadi inayosubiri
    public Optional<CustomerResponse> redeem(Long id) {
        Customer c = customers.findById(id).orElse(null);
        if (c == null) return Optional.empty();
        if (stamps.summary(c).getRewardsAvailable() < 1) {
            throw new BadRequestException("Hakuna zawadi inayosubiri");
        }
        c.setRewardsRedeemed(c.getRewardsRedeemed() + 1);
        customers.save(c);
        rewards.record(c);
        return Optional.of(toResponse(c));
    }

    public CustomerInfoResponse info(Customer c) {
        StampSummary s = stamps.summary(c);
        CustomerInfoResponse r = new CustomerInfoResponse();
        r.setName(c.getName());
        r.setProgress(s.getProgress());
        r.setTarget(StampService.TARGET);
        r.setRewardsAvailable(s.getRewardsAvailable());
        r.setTotalDumu(s.getTotalDumu());
        orderRepository.findByCustomerIdOrderByCreatedAtDesc(c.getId()).stream().findFirst().ifPresent(o -> {
            r.setLastOrderQuantity(o.getQuantity());
            r.setLastOrderAt(o.getCreatedAt());
            r.setLastOrderStatus(o.getStatus());
        });
        return r;
    }

    public CustomerResponse toResponse(Customer c) {
        StampSummary s = stamps.summary(c);
        CustomerResponse r = new CustomerResponse();
        r.setId(c.getId());
        r.setName(c.getName());
        r.setPhone(c.getPhone());
        r.setLocation(c.getLocation());
        r.setToken(c.getToken());
        r.setCreatedAt(c.getCreatedAt());
        r.setTotalDumu(s.getTotalDumu());
        r.setProgress(s.getProgress());
        r.setRewardsAvailable(s.getRewardsAvailable());
        return r;
    }
}
