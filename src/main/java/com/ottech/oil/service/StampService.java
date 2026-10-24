package com.ottech.oil.service;

import com.ottech.oil.dto.StampSummary;
import com.ottech.oil.entity.Customer;
import com.ottech.oil.entity.CustomerOrder;
import com.ottech.oil.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StampService {

    // dumu 6 (lita 20 kila moja) = lita 1 bure
    public static final int TARGET = 6;

    @Autowired
    private OrderRepository orders;

    // Madumu yanahesabiwa yakishapelekwa (DELIVERED) tu
    public StampSummary summary(Customer c) {
        int total = orders.findByCustomerIdAndStatus(c.getId(), "DELIVERED")
                .stream().mapToInt(CustomerOrder::getQuantity).sum();
        StampSummary s = new StampSummary();
        s.setTotalDumu(total);
        s.setProgress(total % TARGET);
        s.setRewardsAvailable(Math.max(0, total / TARGET - c.getRewardsRedeemed()));
        return s;
    }
}
