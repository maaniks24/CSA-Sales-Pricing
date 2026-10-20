package com.levantare.pricing.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "proposal_software")
@Getter
@Setter
public class ProposalSoftware {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposal_id")
    private Proposal proposal;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "software_id")
    private Software software;
    private int assetsCovered;
    private BigDecimal price = BigDecimal.ZERO;
    private BigDecimal implementationCharge = BigDecimal.ZERO;
}
