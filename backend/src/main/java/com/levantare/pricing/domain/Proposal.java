package com.levantare.pricing.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "CsaProposal")
@Table(name = "proposal")
@Getter
@Setter
public class Proposal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int seqNo;
    private String refNo;
    private int version = 1;
    private LocalDate proposalDate;
    @Enumerated(EnumType.STRING)
    private ProposalStatus status = ProposalStatus.SAVED;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;
    private String contactName;
    private int assetCount;
    private int branchCount;
    private BigDecimal srExecDays = BigDecimal.ZERO;
    private BigDecimal discountPercent = BigDecimal.ZERO;
    private String notes;

    @Embedded
    private PricingParams params = new PricingParams();
    @Embedded
    private DocTerms terms = new DocTerms();

    private BigDecimal roomTotal = BigDecimal.ZERO;
    private BigDecimal foodTotal = BigDecimal.ZERO;
    private BigDecimal salaryTotal = BigDecimal.ZERO;
    private BigDecimal branchCost = BigDecimal.ZERO;
    private BigDecimal srExecCost = BigDecimal.ZERO;
    private BigDecimal servicesSubtotal = BigDecimal.ZERO;
    private BigDecimal overheadAmount = BigDecimal.ZERO;
    private BigDecimal profitAmount = BigDecimal.ZERO;
    private BigDecimal servicesPrice = BigDecimal.ZERO;
    private BigDecimal ratePerAsset = BigDecimal.ZERO;
    private BigDecimal softwareTotal = BigDecimal.ZERO;
    private BigDecimal grossTotal = BigDecimal.ZERO;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private BigDecimal netTotal = BigDecimal.ZERO;
    private BigDecimal gstAmount = BigDecimal.ZERO;
    private BigDecimal grandTotal = BigDecimal.ZERO;

    private String poNo;
    private LocalDate poDate;
    private Instant createdAt;
    private Instant updatedAt;

    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<ProposalSoftware> softwareItems = new ArrayList<>();

    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceNo, id")
    private List<ProposalService> serviceItems = new ArrayList<>();
}
