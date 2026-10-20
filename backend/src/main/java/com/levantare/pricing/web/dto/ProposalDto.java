package com.levantare.pricing.web.dto;

import com.levantare.pricing.domain.DocTerms;
import com.levantare.pricing.domain.PricingParams;
import com.levantare.pricing.domain.ProposalStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProposalDto(
        Long id, String refNo, int version, LocalDate proposalDate, ProposalStatus status,
        Long customerId, String customerName, String contactName,
        int assetCount, int branchCount, BigDecimal srExecDays, BigDecimal discountPercent, String notes,
        PricingParams params, DocTerms terms,
        List<SoftwareLineDto> software, List<ServiceLineDto> services,
        Totals totals, String poNo, LocalDate poDate) {

    public record SoftwareLineDto(Long softwareId, String code, String name, int assetsCovered,
                                  BigDecimal price, BigDecimal implementationCharge) {}

    public record ServiceLineDto(Long serviceId, String code, String name, boolean inScope, BigDecimal assetsPerDay,
                                 BigDecimal effortDays, BigDecimal roomCost, BigDecimal foodCost,
                                 BigDecimal salaryCost, BigDecimal totalCost, int workingDays) {}

    public record Totals(BigDecimal roomTotal, BigDecimal foodTotal, BigDecimal salaryTotal,
                         BigDecimal branchCost, BigDecimal travelCost, BigDecimal srExecCost,
                         BigDecimal servicesSubtotal, BigDecimal overheadAmount, BigDecimal profitAmount,
                         BigDecimal servicesPrice, BigDecimal ratePerAsset,
                         BigDecimal softwareTotal, BigDecimal grossTotal, BigDecimal discountAmount,
                         BigDecimal netTotal, BigDecimal gstAmount, BigDecimal grandTotal) {}
}
