package com.levantare.pricing.service;

import com.levantare.pricing.domain.DocSequence;
import com.levantare.pricing.repo.DocSequenceRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SequenceService {
    private final DocSequenceRepository repo;

    public SequenceService(DocSequenceRepository repo) {
        this.repo = repo;
    }

    /** Takes the next number of the series; the first number of a new series is {@code startNo}. */
    @Transactional
    public long next(String key, long startNo) {
        DocSequence seq = lock(key, startNo);
        seq.setLastValue(seq.getLastValue() + 1);
        return seq.getLastValue();
    }

    /** The number {@link #next} would hand out, without consuming it. */
    @Transactional(readOnly = true)
    public long peek(String key, long startNo) {
        return repo.findById(key).map(s -> s.getLastValue() + 1).orElse(startNo);
    }

    /** Makes {@code next} the number handed out by the next call. */
    @Transactional
    public void setNext(String key, long next) {
        DocSequence seq = lock(key, next);
        seq.setLastValue(next - 1);
    }

    private DocSequence lock(String key, long startNo) {
        return repo.lockByKey(key).orElseGet(() -> {
            DocSequence s = new DocSequence();
            s.setSeqKey(key);
            s.setLastValue(startNo - 1);
            return repo.saveAndFlush(s);
        });
    }

    /** Indian financial year, e.g. 2026-27 for any date between 1 Apr 2026 and 31 Mar 2027. */
    public static String financialYear(LocalDate d) {
        int start = d.getMonthValue() >= 4 ? d.getYear() : d.getYear() - 1;
        return start + "-" + String.format("%02d", (start + 1) % 100);
    }
}
