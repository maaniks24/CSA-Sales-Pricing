package com.levantare.pricing.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/** One service phase of a proposal with its calculated effort and cost. */
@Entity(name = "CsaProposalService")
@Table(name = "proposal_service")
@Getter
@Setter
public class ProposalService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposal_id")
    private Proposal proposal;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "service_id")
    private ServiceItem service;
    private int sequenceNo;
    private boolean inScope;
    private BigDecimal assetsPerDay;
    private BigDecimal effortDays = BigDecimal.ZERO;
    private BigDecimal roomCost = BigDecimal.ZERO;
    private BigDecimal foodCost = BigDecimal.ZERO;
    private BigDecimal salaryCost = BigDecimal.ZERO;
    private BigDecimal totalCost = BigDecimal.ZERO;
    private int workingDays;
}
