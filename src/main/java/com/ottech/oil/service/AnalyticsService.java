package com.ottech.oil.service;

import com.ottech.oil.dto.AnalyticsResponse;
import com.ottech.oil.dto.AnalyticsTotals;
import com.ottech.oil.dto.ChartPoint;
import com.ottech.oil.entity.Customer;
import com.ottech.oil.entity.CustomerOrder;
import com.ottech.oil.entity.PriceSetting;
import com.ottech.oil.entity.RewardRedemption;
import com.ottech.oil.repository.CustomerRepository;
import com.ottech.oil.repository.OrderRepository;
import com.ottech.oil.repository.RewardRedemptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Takwimu za oda, wateja wapya na fedha kwa siku, wiki au mwezi
@Service
public class AnalyticsService {

    private enum Step {
        DAILY(14) {
            LocalDate floor(LocalDate d) { return d; }
            LocalDate plus(LocalDate d, int n) { return d.plusDays(n); }
        },
        WEEKLY(12) {
            LocalDate floor(LocalDate d) { return d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)); }
            LocalDate plus(LocalDate d, int n) { return d.plusWeeks(n); }
        },
        MONTHLY(12) {
            LocalDate floor(LocalDate d) { return d.withDayOfMonth(1); }
            LocalDate plus(LocalDate d, int n) { return d.plusMonths(n); }
        };

        final int length;

        Step(int length) { this.length = length; }

        abstract LocalDate floor(LocalDate d);

        abstract LocalDate plus(LocalDate d, int n);
    }

    private static final String[] STATUS_ORDER = {"NEW", "CONFIRMED", "DELIVERED", "CANCELLED"};
    private static final String[] STATUS_NAME = {"Mpya", "Imethibitishwa", "Imepelekwa", "Imekataliwa"};
    private static final String[] SIZE_NAME = {"1", "2", "3", "4-5", "6-9", "10+"};

    @Autowired
    private OrderRepository orders;
    @Autowired
    private CustomerRepository customers;
    @Autowired
    private RewardRedemptionRepository redemptions;
    @Autowired
    private PriceService prices;

    public AnalyticsResponse build(String period) {
        Step step = parse(period);
        int n = step.length;
        int total = 2 * n; // nusu ya kwanza = kipindi kilichopita, nusu ya pili = kipindi cha sasa

        LocalDate last = step.floor(LocalDate.now());
        LocalDate first = step.plus(last, -(total - 1));
        List<LocalDate> starts = new ArrayList<>();
        Map<LocalDate, Integer> index = new HashMap<>();
        for (int i = 0; i < total; i++) {
            LocalDate d = step.plus(first, i);
            starts.add(d);
            index.put(d, i);
        }

        long[] ordersCnt = new long[total];
        long[] activeOrders = new long[total];
        long[] createdDelivered = new long[total];
        long[] dumuOrdered = new long[total];
        long[] dumuDelivered = new long[total];
        long[] newCustomers = new long[total];
        long[] revenue = new long[total];
        long[] cost = new long[total];

        long[] statusCount = new long[STATUS_ORDER.length];
        long[] sizeCount = new long[SIZE_NAME.length];
        Map<String, Long> topCustomers = new LinkedHashMap<>();

        PriceSetting cur = prices.current();

        for (CustomerOrder o : orders.findAll()) {
            Integer i = bucket(index, step, o.getCreatedAt());
            if (i != null) {
                ordersCnt[i]++;
                if (!"CANCELLED".equals(o.getStatus())) {
                    activeOrders[i]++;
                    dumuOrdered[i] += o.getQuantity();
                    if ("DELIVERED".equals(o.getStatus())) createdDelivered[i]++;
                    if (i >= n) sizeCount[sizeBucket(o.getQuantity())]++;
                }
                if (i >= n) {
                    for (int s = 0; s < STATUS_ORDER.length; s++) {
                        if (STATUS_ORDER[s].equals(o.getStatus())) statusCount[s]++;
                    }
                }
            }
            if ("DELIVERED".equals(o.getStatus())) {
                LocalDateTime at = o.getDeliveredAt() != null ? o.getDeliveredAt() : o.getCreatedAt();
                Integer j = bucket(index, step, at);
                if (j != null) {
                    long unitPrice = o.getUnitPrice() != null ? o.getUnitPrice()
                            : cur.getSellPricePerLitre() * PriceService.LITRES_PER_DUMU;
                    long unitCost = o.getUnitCost() != null ? o.getUnitCost()
                            : cur.getBuyPricePerDumu() + cur.getExtraCostPerDumu();
                    dumuDelivered[j] += o.getQuantity();
                    revenue[j] += unitPrice * o.getQuantity();
                    cost[j] += unitCost * o.getQuantity();
                    if (j >= n) topCustomers.merge(o.getCustomerName(), (long) o.getQuantity(), Long::sum);
                }
            }
        }
        for (Customer c : customers.findAll()) {
            Integer i = bucket(index, step, c.getCreatedAt());
            if (i != null) newCustomers[i]++;
        }
        for (RewardRedemption r : redemptions.findAll()) {
            Integer j = bucket(index, step, r.getCreatedAt());
            if (j != null) cost[j] += r.getCost(); // gharama ya zawadi inahesabiwa kama gharama
        }

        AnalyticsResponse res = new AnalyticsResponse();
        res.setPeriod(step.name().toLowerCase());
        res.setRangeLength(n);
        res.setLabels(starts.subList(n, total).stream().map(LocalDate::toString).toList());
        res.setOrders(slice(ordersCnt, n, total));
        res.setDumuOrdered(slice(dumuOrdered, n, total));
        res.setDumuDelivered(slice(dumuDelivered, n, total));
        res.setNewCustomers(slice(newCustomers, n, total));
        res.setRevenue(slice(revenue, n, total));
        res.setCost(slice(cost, n, total));
        List<Long> profit = new ArrayList<>();
        for (int i = n; i < total; i++) profit.add(revenue[i] - cost[i]);
        res.setProfit(profit);

        res.setCurrent(totals(n, total, ordersCnt, activeOrders, createdDelivered, dumuOrdered, dumuDelivered, newCustomers, revenue, cost));
        res.setPrevious(totals(0, n, ordersCnt, activeOrders, createdDelivered, dumuOrdered, dumuDelivered, newCustomers, revenue, cost));

        List<ChartPoint> byStatus = new ArrayList<>();
        for (int s = 0; s < STATUS_ORDER.length; s++) byStatus.add(new ChartPoint(STATUS_NAME[s], statusCount[s]));
        res.setOrdersByStatus(byStatus);

        res.setTopCustomers(topCustomers.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(8)
                .map(e -> new ChartPoint(e.getKey(), e.getValue()))
                .toList());

        List<ChartPoint> sizes = new ArrayList<>();
        for (int s = 0; s < SIZE_NAME.length; s++) sizes.add(new ChartPoint(SIZE_NAME[s], sizeCount[s]));
        res.setOrderSizes(sizes);
        return res;
    }

    private Step parse(String period) {
        if ("weekly".equalsIgnoreCase(period)) return Step.WEEKLY;
        if ("monthly".equalsIgnoreCase(period)) return Step.MONTHLY;
        return Step.DAILY;
    }

    private Integer bucket(Map<LocalDate, Integer> index, Step step, LocalDateTime t) {
        if (t == null) return null;
        return index.get(step.floor(t.toLocalDate()));
    }

    private int sizeBucket(int q) {
        if (q <= 1) return 0;
        if (q == 2) return 1;
        if (q == 3) return 2;
        if (q <= 5) return 3;
        if (q <= 9) return 4;
        return 5;
    }

    private List<Long> slice(long[] a, int from, int to) {
        List<Long> out = new ArrayList<>();
        for (int i = from; i < to; i++) out.add(a[i]);
        return out;
    }

    private long sum(long[] a, int from, int to) {
        long s = 0;
        for (int i = from; i < to; i++) s += a[i];
        return s;
    }

    private AnalyticsTotals totals(int from, int to, long[] orders, long[] active, long[] createdDelivered,
                                   long[] dumuOrdered, long[] dumuDelivered, long[] newCustomers,
                                   long[] revenue, long[] cost) {
        AnalyticsTotals t = new AnalyticsTotals();
        t.setOrders(sum(orders, from, to));
        t.setDumuOrdered(sum(dumuOrdered, from, to));
        t.setDumuDelivered(sum(dumuDelivered, from, to));
        t.setNewCustomers(sum(newCustomers, from, to));
        t.setRevenue(sum(revenue, from, to));
        t.setCost(sum(cost, from, to));
        t.setProfit(t.getRevenue() - t.getCost());
        long act = sum(active, from, to);
        t.setAvgDumuPerOrder(act > 0 ? (double) t.getDumuOrdered() / act : 0);
        t.setDeliveryRate(act > 0 ? 100.0 * sum(createdDelivered, from, to) / act : 0);
        return t;
    }
}
