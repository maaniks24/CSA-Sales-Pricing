package com.levantare.pricing.service;

import com.levantare.pricing.domain.ProposalStatus;
import com.levantare.pricing.repo.*;
import com.levantare.pricing.web.dto.DashboardDto;
import com.levantare.pricing.web.dto.DashboardDto.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final ProposalRepository proposals;
    private final CustomerRepository customers;
    private final SoftwareRepository software;
    private final ServiceItemRepository services;

    public DashboardService(ProposalRepository proposals, CustomerRepository customers, SoftwareRepository software,
                            ServiceItemRepository services) {
        this.proposals = proposals;
        this.customers = customers;
        this.software = software;
        this.services = services;
    }

    @Transactional(readOnly = true)
    public DashboardDto load() {
        List<StatusTotal> byStatus = new ArrayList<>();
        long count = 0, poCount = 0;
        BigDecimal total = BigDecimal.ZERO, poTotal = BigDecimal.ZERO;
        for (Object[] r : proposals.totalsByStatus()) {
            ProposalStatus st = (ProposalStatus) r[0];
            long c = (Long) r[1];
            BigDecimal amt = (BigDecimal) r[2];
            byStatus.add(new StatusTotal(st.name(), c, amt));
            count += c;
            total = total.add(amt);
            if (st == ProposalStatus.PO_GENERATED) {
                poCount += c;
                poTotal = poTotal.add(amt);
            }
        }

        // last six months, including empty ones so the chart has a continuous axis
        YearMonth now = YearMonth.now();
        Map<YearMonth, long[]> counts = new LinkedHashMap<>();
        Map<YearMonth, BigDecimal> amounts = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            counts.put(now.minusMonths(i), new long[1]);
            amounts.put(now.minusMonths(i), BigDecimal.ZERO);
        }
        for (Object[] r : proposals.dateAndTotalSince(now.minusMonths(5).atDay(1))) {
            YearMonth ym = YearMonth.from((LocalDate) r[0]);
            if (counts.containsKey(ym)) {
                counts.get(ym)[0]++;
                amounts.merge(ym, (BigDecimal) r[1], BigDecimal::add);
            }
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yy", Locale.ENGLISH);
        List<MonthTotal> monthly = counts.entrySet().stream()
                .map(e -> new MonthTotal(e.getKey().format(fmt), e.getValue()[0], amounts.get(e.getKey()))).toList();

        List<CustomerTotal> top = proposals.totalsByCustomer().stream().limit(5)
                .map(r -> new CustomerTotal((String) r[0], (Long) r[1], (BigDecimal) r[2])).toList();

        return new DashboardDto(customers.count(), software.count(), services.count(), count, poCount, total, poTotal,
                byStatus, monthly, top,
                proposals.findTop8ByOrderByIdDesc().stream().map(ProposalsService::summary).toList());
    }
}
