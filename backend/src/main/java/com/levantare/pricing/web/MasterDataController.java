package com.levantare.pricing.web;

import com.levantare.pricing.domain.*;
import com.levantare.pricing.repo.*;
import com.levantare.pricing.service.BusinessException;
import com.levantare.pricing.service.NotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** CRUD for the customer, software and service masters plus the global pricing configuration. */
@RestController
@RequestMapping("/api")
public class MasterDataController {

    private final CustomerRepository customers;
    private final SoftwareRepository software;
    private final ServiceItemRepository services;
    private final PricingConfigRepository config;
    private final com.levantare.pricing.service.NumberingService numbering;

    public MasterDataController(CustomerRepository customers, SoftwareRepository software,
                                ServiceItemRepository services, PricingConfigRepository config,
                                com.levantare.pricing.service.NumberingService numbering) {
        this.customers = customers;
        this.software = software;
        this.services = services;
        this.config = config;
        this.numbering = numbering;
    }

    // ---- customers ----
    @GetMapping("/customers")
    public List<Customer> customers() {
        return customers.findAll(Sort.by("name"));
    }

    @GetMapping("/customers/{id}")
    public Customer customer(@PathVariable Long id) {
        return customers.findById(id).orElseThrow(() -> new NotFoundException("Customer", id));
    }

    @PostMapping("/customers")
    @ResponseStatus(HttpStatus.CREATED)
    public Customer createCustomer(@Valid @RequestBody Customer c) {
        if (customers.existsByCodeIgnoreCase(c.getCode())) throw new BusinessException("Customer code already exists");
        c.setId(null);
        return customers.save(c);
    }

    @PutMapping("/customers/{id}")
    public Customer updateCustomer(@PathVariable Long id, @Valid @RequestBody Customer c) {
        customer(id);
        if (customers.existsByCodeIgnoreCaseAndIdNot(c.getCode(), id)) throw new BusinessException("Customer code already exists");
        c.setId(id);
        return customers.save(c);
    }

    @DeleteMapping("/customers/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCustomer(@PathVariable Long id) {
        customers.delete(customer(id));
        customers.flush();
    }

    // ---- software ----
    @GetMapping("/software")
    public List<Software> software() {
        return software.findAll(Sort.by("name"));
    }

    @PostMapping("/software")
    @ResponseStatus(HttpStatus.CREATED)
    public Software createSoftware(@Valid @RequestBody Software s) {
        if (software.existsByCodeIgnoreCase(s.getCode())) throw new BusinessException("Software code already exists");
        s.setId(null);
        return software.save(s);
    }

    @PutMapping("/software/{id}")
    public Software updateSoftware(@PathVariable Long id, @Valid @RequestBody Software s) {
        software.findById(id).orElseThrow(() -> new NotFoundException("Software", id));
        if (software.existsByCodeIgnoreCaseAndIdNot(s.getCode(), id)) throw new BusinessException("Software code already exists");
        s.setId(id);
        return software.save(s);
    }

    @DeleteMapping("/software/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSoftware(@PathVariable Long id) {
        software.delete(software.findById(id).orElseThrow(() -> new NotFoundException("Software", id)));
        software.flush();
    }

    // ---- services ----
    @GetMapping("/services")
    public List<ServiceItem> services() {
        return services.findAll(Sort.by("sequenceNo", "name"));
    }

    @PostMapping("/services")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceItem createService(@Valid @RequestBody ServiceItem s) {
        if (services.existsByCodeIgnoreCase(s.getCode())) throw new BusinessException("Service code already exists");
        s.setId(null);
        return services.save(s);
    }

    @PutMapping("/services/{id}")
    public ServiceItem updateService(@PathVariable Long id, @Valid @RequestBody ServiceItem s) {
        services.findById(id).orElseThrow(() -> new NotFoundException("Service", id));
        if (services.existsByCodeIgnoreCaseAndIdNot(s.getCode(), id)) throw new BusinessException("Service code already exists");
        s.setId(id);
        return services.save(s);
    }

    @DeleteMapping("/services/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteService(@PathVariable Long id) {
        services.delete(services.findById(id).orElseThrow(() -> new NotFoundException("Service", id)));
        services.flush();
    }

    // ---- pricing configuration ----
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

    // ---- reference numbering ----
    @GetMapping("/numbering")
    public com.levantare.pricing.web.dto.NumberingDto numbering() {
        return numbering.view();
    }

    @PutMapping("/numbering")
    public com.levantare.pricing.web.dto.NumberingDto updateNumbering(
            @Valid @RequestBody com.levantare.pricing.web.dto.NumberingDto n) {
        return numbering.update(n);
    }
}
