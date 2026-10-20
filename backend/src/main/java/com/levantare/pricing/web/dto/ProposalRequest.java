package com.levantare.pricing.web.dto;

import com.levantare.pricing.domain.DocTerms;
import com.levantare.pricing.domain.PricingParams;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Input of both "calculate" (preview) and "save". */
public record ProposalRequest(
        @NotNull Long customerId,
        @Size(max = 150) String contactName,
        LocalDate proposalDate,
        @Min(0) Integer assetCount,
        @Min(1) Integer branchCount,
        @DecimalMin("0") BigDecimal srExecDays,
        @DecimalMin("0") @DecimalMax("100") BigDecimal discountPercent,
        @Size(max = 2000) String notes,
        @Valid PricingParams params,
        @Valid DocTerms terms,
        @Valid List<SoftwareLine> software,
        @Valid List<ServiceLine> services) {

    public record SoftwareLine(@NotNull Long softwareId, @Min(0) Integer assetsCovered) {}

    public record ServiceLine(@NotNull Long serviceId, boolean inScope, @DecimalMin("0.01") BigDecimal assetsPerDay) {}
}
