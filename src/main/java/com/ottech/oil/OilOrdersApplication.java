package com.ottech.oil;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class OilOrdersApplication {
    public static void main(String[] args) {
        SpringApplication.run(OilOrdersApplication.class, args);
    }
}
