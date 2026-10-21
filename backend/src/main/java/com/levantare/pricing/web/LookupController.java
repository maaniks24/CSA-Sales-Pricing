package com.levantare.pricing.web;

import com.levantare.pricing.domain.Customer;
import com.levantare.pricing.domain.ServiceItem;
import com.levantare.pricing.domain.Software;
import com.levantare.pricing.repo.CustomerRepository;
import com.levantare.pricing.repo.ServiceItemRepository;
import com.levantare.pricing.repo.SoftwareRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only lists the proposal screen needs. Always enabled - in a host application the entities behind them are
 * mapped to the host's own master data (see docs/INTEGRATION-GUIDE.md), so the Angular proposal screen needs no change.
 */
@RestController
@RequestMapping("/api/lookups")
public class LookupController {

    private final CustomerRepository customers;
    private final SoftwareRepository software;
    private final ServiceItemRepository services;

    public LookupController(CustomerRepository customers, SoftwareRepository software, ServiceItemRepository services) {
        this.customers = customers;
        this.software = software;
        this.services = services;
    }

    @GetMapping("/customers")
    public List<Customer> customers() {
        return customers.findAll(Sort.by("name"));
    }

    @GetMapping("/software")
    public List<Software> software() {
        return software.findAll(Sort.by("name"));
    }

    @GetMapping("/services")
    public List<ServiceItem> services() {
        return services.findAll(Sort.by("sequenceNo", "name"));
    }
}
