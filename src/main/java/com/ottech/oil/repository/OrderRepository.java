package com.ottech.oil.repository;

import com.ottech.oil.entity.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {
    boolean existsByCustomerIdAndStatus(Long customerId, String status);
    List<CustomerOrder> findAllByOrderByCreatedAtDesc();
    List<CustomerOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<CustomerOrder> findByCustomerIdAndStatus(Long customerId, String status);
    List<CustomerOrder> findByStatus(String status);
    long countByStatus(String status);
}
