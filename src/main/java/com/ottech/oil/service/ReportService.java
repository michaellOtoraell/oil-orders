package com.ottech.oil.service;

import com.ottech.oil.dto.PeriodSummary;
import com.ottech.oil.dto.PriceResponse;
import com.ottech.oil.dto.ReportResponse;
import com.ottech.oil.entity.CustomerOrder;
import com.ottech.oil.entity.PriceSetting;
import com.ottech.oil.entity.RewardRedemption;
import com.ottech.oil.repository.OrderRepository;
import com.ottech.oil.repository.RewardRedemptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReportService {

    @Autowired
    private OrderRepository orders;
    @Autowired
    private RewardRedemptionRepository redemptions;
    @Autowired
    private PriceService prices;

    public ReportResponse report() {
        PriceSetting cur = prices.current();
        PriceResponse curResponse = prices.toResponse(cur);
        List<CustomerOrder> delivered = orders.findByStatus("DELIVERED");
        List<RewardRedemption> rewards = redemptions.findAll();

        LocalDate today = LocalDate.now();
        ReportResponse r = new ReportResponse();
        r.setPeriods(List.of(
                summarize("Leo", today.atStartOfDay(), delivered, rewards, cur),
                summarize("Siku 7", today.minusDays(6).atStartOfDay(), delivered, rewards, cur),
                summarize("Mwezi huu", today.withDayOfMonth(1).atStartOfDay(), delivered, rewards, cur),
                summarize("Jumla", null, delivered, rewards, cur)));
        r.setPrice(curResponse);

        long pending = orders.findByStatus("NEW").stream().mapToLong(CustomerOrder::getQuantity).sum()
                + orders.findByStatus("CONFIRMED").stream().mapToLong(CustomerOrder::getQuantity).sum();
        r.setPendingDumu(pending);
        r.setExpectedProfit(pending * curResponse.getProfitPerDumu());
        return r;
    }

    // from = null maana tangu mwanzo. Oda za zamani zisizo na bei zilizohifadhiwa zinatumia bei za sasa.
    private PeriodSummary summarize(String label, LocalDateTime from, List<CustomerOrder> delivered,
                                    List<RewardRedemption> rewards, PriceSetting cur) {
        long dumu = 0, revenue = 0, cost = 0, rewardsCost = 0;
        for (CustomerOrder o : delivered) {
            LocalDateTime at = o.getDeliveredAt() != null ? o.getDeliveredAt() : o.getCreatedAt();
            if (from != null && at != null && at.isBefore(from)) continue;
            long unitPrice = o.getUnitPrice() != null ? o.getUnitPrice()
                    : cur.getSellPricePerLitre() * PriceService.LITRES_PER_DUMU;
            long unitCost = o.getUnitCost() != null ? o.getUnitCost()
                    : cur.getBuyPricePerDumu() + cur.getExtraCostPerDumu();
            dumu += o.getQuantity();
            revenue += unitPrice * o.getQuantity();
            cost += unitCost * o.getQuantity();
        }
        for (RewardRedemption w : rewards) {
            if (from != null && w.getCreatedAt() != null && w.getCreatedAt().isBefore(from)) continue;
            rewardsCost += w.getCost();
        }
        PeriodSummary s = new PeriodSummary();
        s.setLabel(label);
        s.setDumu(dumu);
        s.setRevenue(revenue);
        s.setCost(cost);
        s.setRewardsCost(rewardsCost);
        s.setProfit(revenue - cost - rewardsCost);
        return s;
    }
}
