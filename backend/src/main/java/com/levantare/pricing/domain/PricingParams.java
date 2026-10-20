package com.levantare.pricing.domain;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Rate card and cost loadings. Lives in the global config and is snapshotted on every proposal. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class PricingParams {
    @DecimalMin("0") private BigDecimal trackingRoom = BigDecimal.ZERO;
    @DecimalMin("0") private BigDecimal trackingFood = BigDecimal.ZERO;
    @DecimalMin("0") private BigDecimal trackingSalary = BigDecimal.ZERO;
    @DecimalMin("0") private BigDecimal dataAuditSalary = BigDecimal.ZERO;
    @DecimalMin("0") private BigDecimal srExecRoom = BigDecimal.ZERO;
    @DecimalMin("0") private BigDecimal srExecFood = BigDecimal.ZERO;
    @DecimalMin("0") private BigDecimal srExecSalary = BigDecimal.ZERO;
    @DecimalMin("0") private BigDecimal costPerBranch = BigDecimal.ZERO;
    @DecimalMin("0") private BigDecimal travelCost = BigDecimal.ZERO;
    @DecimalMin("0") private BigDecimal overheadPercent = BigDecimal.ZERO;
    @DecimalMin("0") private BigDecimal profitPercent = BigDecimal.ZERO;

    public PricingParams copy() {
        PricingParams c = new PricingParams();
        c.trackingRoom = trackingRoom; c.trackingFood = trackingFood; c.trackingSalary = trackingSalary;
        c.dataAuditSalary = dataAuditSalary; c.srExecRoom = srExecRoom; c.srExecFood = srExecFood;
        c.srExecSalary = srExecSalary; c.costPerBranch = costPerBranch; c.travelCost = travelCost;
        c.overheadPercent = overheadPercent; c.profitPercent = profitPercent;
        return c;
    }
}
