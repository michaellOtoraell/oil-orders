package com.ottech.oil.service;

import com.ottech.oil.dto.OrderResponse;
import com.ottech.oil.entity.Customer;
import com.ottech.oil.entity.CustomerOrder;
import com.ottech.oil.entity.PriceSetting;
import com.ottech.oil.exception.BadRequestException;
import com.ottech.oil.exception.ConflictException;
import com.ottech.oil.repository.CustomerRepository;
import com.ottech.oil.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class OrderService {

    private static final Set<String> STATUSES = Set.of("NEW", "CONFIRMED", "DELIVERED", "CANCELLED");

    @Autowired
    private OrderRepository orders;
    @Autowired
    private CustomerRepository customers;
    @Autowired
    private StampService stamps;
    @Autowired
    private EmailService email;
    @Autowired
    private PriceService prices;

    public OrderResponse place(Customer c, int quantity) {
        if (quantity < 1 || quantity > 100) {
            throw new BadRequestException("Idadi si sahihi");
        }
        if (orders.existsByCustomerIdAndStatus(c.getId(), "NEW")) {
            throw new ConflictException("Una oda inayosubiri tayari. Tutakupigia simu hivi karibuni.");
        }
        CustomerOrder o = new CustomerOrder();
        o.setCustomerId(c.getId());
        o.setCustomerName(c.getName());
        o.setCustomerPhone(c.getPhone());
        o.setCustomerLocation(c.getLocation());
        o.setQuantity(quantity);
        o.setStatus("NEW");
        o.setCreatedAt(LocalDateTime.now());
        orders.save(o);

        email.notifyNewOrder(o);
        return toResponse(o, 0);
    }

    public List<OrderResponse> history(Customer c) {
        int rewards = rewardsFor(c.getId());
        return orders.findByCustomerIdOrderByCreatedAtDesc(c.getId()).stream()
                .map(o -> toResponse(o, rewards))
                .toList();
    }

    public List<OrderResponse> listAll() {
        Map<Long, Integer> cache = new HashMap<>();
        return orders.findAllByOrderByCreatedAtDesc().stream()
                .map(o -> toResponse(o, cache.computeIfAbsent(o.getCustomerId(), this::rewardsFor)))
                .toList();
    }

    public Optional<OrderResponse> updateStatus(Long id, String status) {
        if (!STATUSES.contains(status)) {
            throw new BadRequestException("Status si sahihi");
        }
        CustomerOrder o = orders.findById(id).orElse(null);
        if (o == null) return Optional.empty();
        boolean becomingDelivered = "DELIVERED".equals(status) && !"DELIVERED".equals(o.getStatus());
        o.setStatus(status);
        if (becomingDelivered) {
            // hifadhi bei za wakati huu ili mapato ya zamani yasibadilike bei zikibadilika
            PriceSetting p = prices.current();
            o.setUnitCost(p.getBuyPricePerDumu() + p.getExtraCostPerDumu());
            o.setUnitPrice(p.getSellPricePerLitre() * PriceService.LITRES_PER_DUMU);
            o.setDeliveredAt(LocalDateTime.now());
        }
        return Optional.of(toResponse(orders.save(o), rewardsFor(o.getCustomerId())));
    }

    private int rewardsFor(Long customerId) {
        return customers.findById(customerId)
                .map(c -> stamps.summary(c).getRewardsAvailable())
                .orElse(0);
    }

    private OrderResponse toResponse(CustomerOrder o, int rewardsAvailable) {
        OrderResponse r = new OrderResponse();
        r.setId(o.getId());
        r.setCustomerId(o.getCustomerId());
        r.setCustomerName(o.getCustomerName());
        r.setCustomerPhone(o.getCustomerPhone());
        r.setCustomerLocation(o.getCustomerLocation());
        r.setQuantity(o.getQuantity());
        r.setStatus(o.getStatus());
        r.setCreatedAt(o.getCreatedAt());
        r.setRewardsAvailable(rewardsAvailable);
        return r;
    }
}
