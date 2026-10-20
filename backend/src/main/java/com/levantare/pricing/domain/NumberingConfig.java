package com.levantare.pricing.domain;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Format of the proposal reference and PO numbers: {prefix}/{financial year}/{running no}[-v{version}]. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class NumberingConfig {
    private String proposalPrefix = "LTPL/PRO";
    private String poPrefix = "LTPL/PO";
    private boolean includeFinancialYear = true;
    /** Running numbers restart at {@link #seriesStartNo} for every financial year. */
    private boolean resetEachYear = true;
    /** Digits of the running number; with width 5 the range is 1 - 99999. */
    private int numberWidth = 5;
    /** First number of a new series (first year, or a new financial year when resetting yearly). */
    private int seriesStartNo = 1;

    public long maxNumber() {
        return (long) Math.pow(10, numberWidth) - 1;
    }
}
