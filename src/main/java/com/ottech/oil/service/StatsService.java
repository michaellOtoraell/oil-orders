package com.ottech.oil.service;

import com.ottech.oil.dto.StatsResponse;
import com.ottech.oil.entity.CustomerOrder;
import com.ottech.oil.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StatsService {

    @Autowired
    private OrderRepository orders;

    public StatsResponse stats() {
        List<CustomerOrder> all = orders.findAll();
        StatsResponse s = new StatsResponse();
        s.setTotalOrders(all.size());
        s.setTotalDumu(all.stream()
                .filter(o -> !"CANCELLED".equals(o.getStatus()))
                .mapToLong(CustomerOrder::getQuantity).sum());
        s.setNewOrders(orders.countByStatus("NEW"));
        s.setDeliveredOrders(orders.countByStatus("DELIVERED"));
        return s;
    }
}
