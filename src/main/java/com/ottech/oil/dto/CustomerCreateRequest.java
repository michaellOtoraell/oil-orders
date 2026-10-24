package com.ottech.oil.dto;

import lombok.Data;

@Data
public class CustomerCreateRequest {
    private String name;
    private String phone;
    private String location;
}
