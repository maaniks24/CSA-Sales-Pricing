package com.levantare.pricing.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardDto(long customers, long software, long services,
                           long proposals, long poGenerated, BigDecimal totalValue, BigDecimal poValue,
                           List<StatusTotal> byStatus, List<MonthTotal> monthly, List<CustomerTotal> topCustomers,
                           List<ProposalSummary> recent) {

    public record StatusTotal(String status, long count, BigDecimal amount) {}

    public record MonthTotal(String month, long count, BigDecimal amount) {}

    public record CustomerTotal(String customer, long count, BigDecimal amount) {}
}
