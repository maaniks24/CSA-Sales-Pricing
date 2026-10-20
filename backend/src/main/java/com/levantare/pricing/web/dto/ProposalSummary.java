package com.levantare.pricing.web.dto;

import com.levantare.pricing.domain.ProposalStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ProposalSummary(Long id, String refNo, LocalDate proposalDate, Long customerId, String customerName,
                              ProposalStatus status, BigDecimal grandTotal, String poNo) {}
