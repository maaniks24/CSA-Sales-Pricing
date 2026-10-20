package com.levantare.pricing.service;

import com.levantare.pricing.domain.NumberingConfig;
import com.levantare.pricing.domain.PricingConfig;
import com.levantare.pricing.repo.PricingConfigRepository;
import com.levantare.pricing.web.dto.NumberingDto;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Builds proposal reference and PO numbers from the numbering settings stored in the database. */
@Service
public class NumberingService {

    private static final String PRO = "PRO";
    private static final String PO = "PO";

    private final PricingConfigRepository configRepo;
    private final SequenceService sequences;

    public NumberingService(PricingConfigRepository configRepo, SequenceService sequences) {
        this.configRepo = configRepo;
        this.sequences = sequences;
    }

    private NumberingConfig cfg() {
        return configRepo.findById(PricingConfig.ID).orElseThrow().getNumbering();
    }

    private static String key(String series, NumberingConfig c, LocalDate date) {
        return c.isResetEachYear() ? series + "-" + SequenceService.financialYear(date) : series;
    }

    /** Takes the next proposal number. */
    @Transactional
    public long nextProposalNo(LocalDate date) {
        return take(PRO, date);
    }

    @Transactional
    public long nextPoNo(LocalDate date) {
        return take(PO, date);
    }

    private long take(String series, LocalDate date) {
        NumberingConfig c = cfg();
        long n = sequences.next(key(series, c, date), c.getSeriesStartNo());
        if (n > c.maxNumber()) {
            // give the number back, nothing was issued
            sequences.setNext(key(series, c, date), n);
            throw new BusinessException("Number range exhausted (maximum " + c.maxNumber()
                    + "). Increase the number size in Pricing Settings > Reference numbering.");
        }
        return n;
    }

    public String proposalRef(LocalDate date, long no, int version) {
        return format(cfg().getProposalPrefix(), cfg(), date, no) + "-v" + version;
    }

    public String poNo(LocalDate date, long no) {
        return format(cfg().getPoPrefix(), cfg(), date, no);
    }

    private static String format(String prefix, NumberingConfig c, LocalDate date, long no) {
        String fy = c.isIncludeFinancialYear() ? "/" + SequenceService.financialYear(date) : "";
        return prefix.trim() + fy + "/" + String.format("%0" + c.getNumberWidth() + "d", no);
    }

    @Transactional(readOnly = true)
    public NumberingDto view() {
        return toDto(cfg(), LocalDate.now());
    }

    @Transactional
    public NumberingDto update(NumberingDto in) {
        PricingConfig config = configRepo.findById(PricingConfig.ID).orElseThrow();
        NumberingConfig c = config.getNumbering();
        c.setProposalPrefix(in.proposalPrefix().trim());
        c.setPoPrefix(in.poPrefix().trim());
        c.setIncludeFinancialYear(in.includeFinancialYear());
        c.setResetEachYear(in.resetEachYear());
        c.setNumberWidth(in.numberWidth());
        long max = c.maxNumber();
        if (in.seriesStartNo() > max || in.nextProposalNo() > max || in.nextPoNo() > max) {
            throw new BusinessException("Numbers must be within the range 1 - " + max + " for a number size of "
                    + in.numberWidth() + " digits");
        }
        c.setSeriesStartNo((int) in.seriesStartNo());
        configRepo.save(config);
        LocalDate today = LocalDate.now();
        sequences.setNext(key(PRO, c, today), in.nextProposalNo());
        sequences.setNext(key(PO, c, today), in.nextPoNo());
        return toDto(c, today);
    }

    private NumberingDto toDto(NumberingConfig c, LocalDate today) {
        long nextPro = sequences.peek(key(PRO, c, today), c.getSeriesStartNo());
        long nextPo = sequences.peek(key(PO, c, today), c.getSeriesStartNo());
        return new NumberingDto(c.getProposalPrefix(), c.getPoPrefix(), c.isIncludeFinancialYear(), c.isResetEachYear(),
                c.getNumberWidth(), c.getSeriesStartNo(), nextPro, nextPo, SequenceService.financialYear(today),
                format(c.getProposalPrefix(), c, today, nextPro) + "-v1", format(c.getPoPrefix(), c, today, nextPo),
                c.maxNumber());
    }
}
