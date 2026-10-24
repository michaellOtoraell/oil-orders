package com.ottech.oil.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String phone;
    private String location;
    @Column(unique = true)
    private String token;
    private LocalDateTime createdAt;
    private int rewardsRedeemed;
}
