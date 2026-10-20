package com.levantare.pricing.repo;

import com.levantare.pricing.domain.PricingConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PricingConfigRepository extends JpaRepository<PricingConfig, Long> {
}
