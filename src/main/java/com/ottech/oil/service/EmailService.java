package com.ottech.oil.service;

import com.ottech.oil.entity.CustomerOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

// Inatuma kwa HTTP API (Resend), kwa sababu Render free inazuia SMTP
@Slf4j
@Service
public class EmailService {

    @Value("${app.resend.key:}")
    private String apiKey;

    @Value("${app.notify.email}")
    private String to;

    private final RestClient http = RestClient.create();

    @Async
    public void notifyNewOrder(CustomerOrder o) {
        if (apiKey.isBlank()) {
            log.warn("RESEND_API_KEY haijawekwa, email haikutumwa");
            return;
        }
        try {
            Map<String, Object> body = Map.of(
                    "from", "Ottoil Oda <onboarding@resend.dev>",
                    "to", List.of(to),
                    "subject", "Oda mpya: " + o.getCustomerName() + " - dumu " + o.getQuantity(),
                    "text", "Mteja: " + o.getCustomerName()
                            + "\nSimu: " + o.getCustomerPhone()
                            + "\nEneo: " + o.getCustomerLocation()
                            + "\nIdadi: dumu " + o.getQuantity()
                            + "\n\nMpigie kuthibitisha.");
            http.post().uri("https://api.resend.com/emails")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.error("Email imeshindwa kutumwa kwa oda {}", o.getId(), e);
        }
    }
}
