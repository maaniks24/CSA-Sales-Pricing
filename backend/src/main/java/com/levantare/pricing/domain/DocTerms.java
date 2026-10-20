package com.levantare.pricing.domain;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Commercial terms that end up in the generated document. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class DocTerms {
    @DecimalMin("0") @DecimalMax("100") private BigDecimal gstPercent = new BigDecimal("18");
    @Min(1) private int validityDays = 10;
    @Min(0) private int commencementDays = 10;
    @Min(1) private int trackingTeamSize = 1;
    @Min(1) private int dataTeamSize = 1;
    /** Milestone payment split (%): purchase order / physical verification / completion. */
    @Min(0) @Max(100) private int pay1 = 50;
    @Min(0) @Max(100) private int pay2 = 40;
    @Min(0) @Max(100) private int pay3 = 10;

    public DocTerms copy() {
        DocTerms c = new DocTerms();
        c.gstPercent = gstPercent; c.validityDays = validityDays; c.commencementDays = commencementDays;
        c.trackingTeamSize = trackingTeamSize; c.dataTeamSize = dataTeamSize;
        c.pay1 = pay1; c.pay2 = pay2; c.pay3 = pay3;
        return c;
    }
}
