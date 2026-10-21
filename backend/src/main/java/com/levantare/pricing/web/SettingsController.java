package com.levantare.pricing.web;

import com.levantare.pricing.domain.PricingConfig;
import com.levantare.pricing.repo.PricingConfigRepository;
import com.levantare.pricing.service.NotFoundException;
import com.levantare.pricing.service.NumberingService;
import com.levantare.pricing.web.dto.NumberingDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** Pricing configuration (rate card, loadings, document terms) and reference numbering. Part of the proposal module. */
@RestController
@RequestMapping("/api")
public class SettingsController {

    private final PricingConfigRepository config;
    private final NumberingService numbering;

    public SettingsController(PricingConfigRepository config, NumberingService numbering) {
        this.config = config;
        this.numbering = numbering;
    }

    @GetMapping("/config")
    public PricingConfig config() {
        return config.findById(PricingConfig.ID).orElseThrow(() -> new NotFoundException("Config", PricingConfig.ID));
    }

    @PutMapping("/config")
    public PricingConfig updateConfig(@Valid @RequestBody PricingConfig c) {
        // numbering has its own endpoint, never overwrite it from here
        PricingConfig current = config();
        current.setParams(c.getParams());
        current.setTerms(c.getTerms());
        return config.save(current);
    }

    @GetMapping("/numbering")
    public NumberingDto numbering() {
        return numbering.view();
    }

    @PutMapping("/numbering")
    public NumberingDto updateNumbering(@Valid @RequestBody NumberingDto n) {
        return numbering.update(n);
    }
}
