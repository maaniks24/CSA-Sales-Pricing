package com.levantare.pricing.web.dto;

import jakarta.validation.constraints.*;

/** Numbering settings. The read-only fields are filled by the server and ignored on update. */
public record NumberingDto(
        @NotBlank @Size(max = 40) @Pattern(regexp = "[A-Za-z0-9/_.\\- ]+", message = "may only contain letters, digits and / _ . -") String proposalPrefix,
        @NotBlank @Size(max = 40) @Pattern(regexp = "[A-Za-z0-9/_.\\- ]+", message = "may only contain letters, digits and / _ . -") String poPrefix,
        boolean includeFinancialYear,
        boolean resetEachYear,
        @Min(3) @Max(9) int numberWidth,
        @Min(1) long seriesStartNo,
        @Min(1) long nextProposalNo,
        @Min(1) long nextPoNo,
        String currentFinancialYear,
        String proposalExample,
        String poExample,
        Long maxNumber) {}
