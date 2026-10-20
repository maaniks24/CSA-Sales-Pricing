package com.levantare.pricing.domain;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

/** Single row table (id = 1) holding the defaults used for new proposals. */
@Entity
@Table(name = "pricing_config")
@Getter
@Setter
public class PricingConfig {
    public static final long ID = 1L;
    @Id
    private Long id = ID;
    @Embedded @Valid
    private PricingParams params = new PricingParams();
    @Embedded @Valid
    private DocTerms terms = new DocTerms();
}
