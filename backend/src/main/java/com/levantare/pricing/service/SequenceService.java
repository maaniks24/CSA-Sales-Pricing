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

    /** Next number of the given series; callers must run inside a transaction (row is locked until commit). */
    @Transactional
    public int next(String key) {
        DocSequence seq = repo.lockByKey(key).orElseGet(() -> {
            DocSequence s = new DocSequence();
            s.setSeqKey(key);
            s.setLastValue(0);
            return repo.saveAndFlush(s);
        });
        seq.setLastValue(seq.getLastValue() + 1);
        return (int) seq.getLastValue();
    }

    /** Indian financial year, e.g. 2026-27 for any date between 1 Apr 2026 and 31 Mar 2027. */
    public static String financialYear(LocalDate d) {
        int start = d.getMonthValue() >= 4 ? d.getYear() : d.getYear() - 1;
        return start + "-" + String.format("%02d", (start + 1) % 100);
    }
}
