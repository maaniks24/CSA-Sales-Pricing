package com.levantare.pricing.service;

import com.levantare.pricing.calc.PricingCalculator;
import com.levantare.pricing.calc.PricingCalculator.Result;
import com.levantare.pricing.domain.*;
import com.levantare.pricing.doc.ProposalDocumentBuilder;
import com.levantare.pricing.repo.*;
import com.levantare.pricing.web.dto.*;
import com.levantare.pricing.web.dto.ProposalDto.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProposalsService {

    public record GeneratedDocument(String fileName, byte[] content) {}

    private final ProposalRepository proposals;
    private final CustomerRepository customers;
    private final SoftwareRepository softwareRepo;
    private final ServiceItemRepository serviceRepo;
    private final PricingConfigRepository configRepo;
    private final SequenceService sequences;
    private final ProposalDocumentBuilder documents;
    private final String refPrefix;

    public ProposalsService(ProposalRepository proposals, CustomerRepository customers, SoftwareRepository softwareRepo,
                            ServiceItemRepository serviceRepo, PricingConfigRepository configRepo,
                            SequenceService sequences, ProposalDocumentBuilder documents,
                            @Value("${app.ref-prefix:LTPL}") String refPrefix) {
        this.proposals = proposals;
        this.customers = customers;
        this.softwareRepo = softwareRepo;
        this.serviceRepo = serviceRepo;
        this.configRepo = configRepo;
        this.sequences = sequences;
        this.documents = documents;
        this.refPrefix = refPrefix;
    }

    @Transactional(readOnly = true)
    public List<ProposalSummary> list() {
        return proposals.findAllByOrderByIdDesc().stream().map(ProposalsService::summary).toList();
    }

    @Transactional(readOnly = true)
    public ProposalDto get(Long id) {
        return toDto(find(id));
    }

    /** Calculates without saving - used by the UI to show live totals. */
    @Transactional(readOnly = true)
    public ProposalDto preview(ProposalRequest req) {
        Proposal p = new Proposal();
        apply(p, req);
        return toDto(p);
    }

    @Transactional
    public ProposalDto create(ProposalRequest req) {
        Proposal p = new Proposal();
        apply(p, req);
        requireContent(p);
        p.setProposalDate(req.proposalDate() != null ? req.proposalDate() : LocalDate.now());
        p.setSeqNo(sequences.next("PRO-" + SequenceService.financialYear(p.getProposalDate())));
        p.setVersion(1);
        p.setRefNo(refNo(p));
        p.setStatus(ProposalStatus.SAVED);
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(p.getCreatedAt());
        return toDto(proposals.save(p));
    }

    @Transactional
    public ProposalDto update(Long id, ProposalRequest req) {
        Proposal p = find(id);
        apply(p, req);
        requireContent(p);
        if (req.proposalDate() != null) p.setProposalDate(req.proposalDate());
        if (p.getStatus() == ProposalStatus.PO_GENERATED) {
            // an issued document was changed: new version of the same proposal
            p.setVersion(p.getVersion() + 1);
            p.setStatus(ProposalStatus.SAVED);
        }
        p.setRefNo(refNo(p));
        p.setUpdatedAt(Instant.now());
        return toDto(proposals.save(p));
    }

    @Transactional
    public void delete(Long id) {
        proposals.delete(find(id));
    }

    @Transactional
    public GeneratedDocument generatePo(Long id) throws IOException {
        Proposal p = find(id);
        if (p.getPoNo() == null) {
            String fy = SequenceService.financialYear(LocalDate.now());
            p.setPoNo("%s/PO/%s/%05d".formatted(refPrefix, fy, sequences.next("PO-" + fy)));
            p.setPoDate(LocalDate.now());
        }
        p.setStatus(ProposalStatus.PO_GENERATED);
        p.setUpdatedAt(Instant.now());
        proposals.save(p);
        byte[] bytes = documents.build(p);
        String safeCustomer = p.getCustomer().getName().replaceAll("[^A-Za-z0-9]+", "_").replaceAll("^_+|_+$", "");
        String safeRef = p.getRefNo().replaceAll("[^A-Za-z0-9-]+", "_");
        return new GeneratedDocument(safeCustomer + "_" + safeRef + ".docx", bytes);
    }

    // ---- internals ----------------------------------------------------------------------------------------

    private Proposal find(Long id) {
        return proposals.findById(id).orElseThrow(() -> new NotFoundException("Proposal", id));
    }

    private String refNo(Proposal p) {
        return "%s/PRO/%s/%05d-v%d".formatted(refPrefix, SequenceService.financialYear(p.getProposalDate()),
                p.getSeqNo(), p.getVersion());
    }

    private void requireContent(Proposal p) {
        boolean anyService = p.getServiceItems().stream().anyMatch(ProposalService::isInScope);
        if (p.getSoftwareItems().isEmpty() && !anyService) {
            throw new BusinessException("Add at least one software item or select at least one service in scope");
        }
        if (anyService) {
            DocTerms t = p.getTerms();
            if (t.getPay1() + t.getPay2() + t.getPay3() != 100) {
                throw new BusinessException("Milestone payment percentages must add up to 100");
            }
        }
    }

    /** Copies the request onto the entity (null values fall back to customer / global config) and calculates. */
    private void apply(Proposal p, ProposalRequest req) {
        Customer customer = customers.findById(req.customerId())
                .orElseThrow(() -> new NotFoundException("Customer", req.customerId()));
        PricingConfig config = configRepo.findById(PricingConfig.ID)
                .orElseThrow(() -> new IllegalStateException("Pricing configuration missing"));

        p.setCustomer(customer);
        p.setContactName(blankToNull(req.contactName()) != null ? req.contactName().trim() : customer.getContactName());
        p.setAssetCount(req.assetCount() != null ? req.assetCount() : customer.getAssetCount());
        p.setBranchCount(req.branchCount() != null ? req.branchCount() : customer.getBranchCount());
        p.setSrExecDays(orZero(req.srExecDays()));
        p.setDiscountPercent(req.discountPercent() != null ? req.discountPercent() : customer.getDiscountPercent());
        p.setNotes(req.notes());
        p.setParams(req.params() != null ? req.params() : config.getParams().copy());
        p.setTerms(req.terms() != null ? req.terms() : config.getTerms().copy());
        if (p.getProposalDate() == null) p.setProposalDate(req.proposalDate() != null ? req.proposalDate() : LocalDate.now());

        // software lines
        p.getSoftwareItems().clear();
        List<PricingCalculator.SoftwareIn> swIn = new ArrayList<>();
        for (ProposalRequest.SoftwareLine l : orEmpty(req.software())) {
            Software sw = softwareRepo.findById(l.softwareId()).orElseThrow(() -> new NotFoundException("Software", l.softwareId()));
            ProposalSoftware line = new ProposalSoftware();
            line.setProposal(p);
            line.setSoftware(sw);
            line.setAssetsCovered(l.assetsCovered() != null && l.assetsCovered() > 0 ? l.assetsCovered() : p.getAssetCount());
            p.getSoftwareItems().add(line);
            swIn.add(new PricingCalculator.SoftwareIn(sw.getPrice(), sw.getAssetBlock(), sw.getImplementationCharge(),
                    line.getAssetsCovered()));
        }

        // service lines
        p.getServiceItems().clear();
        List<ProposalService> serviceLines = new ArrayList<>();
        for (ProposalRequest.ServiceLine l : orEmpty(req.services())) {
            ServiceItem s = serviceRepo.findById(l.serviceId()).orElseThrow(() -> new NotFoundException("Service", l.serviceId()));
            ProposalService line = new ProposalService();
            line.setProposal(p);
            line.setService(s);
            line.setSequenceNo(s.getSequenceNo());
            line.setInScope(l.inScope());
            line.setAssetsPerDay(l.assetsPerDay() != null ? l.assetsPerDay() : s.getAssetsPerDay());
            serviceLines.add(line);
        }
        serviceLines.sort(Comparator.comparingInt(ProposalService::getSequenceNo));
        List<PricingCalculator.ServiceIn> svIn = serviceLines.stream().map(line -> new PricingCalculator.ServiceIn(line.isInScope(), line.getAssetsPerDay(),
                line.getService().getResourceType(), line.getService().isIncludeRoom(), line.getService().isIncludeFood())).toList();

        Result r = PricingCalculator.calculate(new PricingCalculator.Input(p.getAssetCount(), p.getBranchCount(),
                p.getSrExecDays(), p.getDiscountPercent(), p.getParams(), p.getTerms(), svIn, swIn));

        for (int i = 0; i < serviceLines.size(); i++) {
            var o = r.services().get(i);
            ProposalService line = serviceLines.get(i);
            line.setEffortDays(o.effortDays());
            line.setRoomCost(o.room());
            line.setFoodCost(o.food());
            line.setSalaryCost(o.salary());
            line.setTotalCost(o.total());
            line.setWorkingDays(o.workingDays());
            p.getServiceItems().add(line);
        }
        for (int i = 0; i < p.getSoftwareItems().size(); i++) {
            p.getSoftwareItems().get(i).setPrice(r.software().get(i).price());
            p.getSoftwareItems().get(i).setImplementationCharge(r.software().get(i).implementationCharge());
        }
        p.setRoomTotal(r.roomTotal());
        p.setFoodTotal(r.foodTotal());
        p.setSalaryTotal(r.salaryTotal());
        p.setBranchCost(r.branchCost());
        p.setSrExecCost(r.srExecCost());
        p.setServicesSubtotal(r.servicesSubtotal());
        p.setOverheadAmount(r.overheadAmount());
        p.setProfitAmount(r.profitAmount());
        p.setServicesPrice(r.servicesPrice());
        p.setRatePerAsset(r.ratePerAsset());
        p.setSoftwareTotal(r.softwareTotal());
        p.setGrossTotal(r.grossTotal());
        p.setDiscountAmount(r.discountAmount());
        p.setNetTotal(r.netTotal());
        p.setGstAmount(r.gstAmount());
        p.setGrandTotal(r.grandTotal());
    }

    static ProposalSummary summary(Proposal p) {
        return new ProposalSummary(p.getId(), p.getRefNo(), p.getProposalDate(), p.getCustomer().getId(),
                p.getCustomer().getName(), p.getStatus(), p.getGrandTotal(), p.getPoNo());
    }

    static ProposalDto toDto(Proposal p) {
        List<SoftwareLineDto> sw = p.getSoftwareItems().stream().map(l -> new SoftwareLineDto(
                l.getSoftware().getId(), l.getSoftware().getCode(), l.getSoftware().getName(), l.getAssetsCovered(),
                l.getPrice(), l.getImplementationCharge())).toList();
        List<ServiceLineDto> sv = p.getServiceItems().stream().map(l -> new ServiceLineDto(
                l.getService().getId(), l.getService().getCode(), l.getService().getName(), l.isInScope(),
                l.getAssetsPerDay(), l.getEffortDays(), l.getRoomCost(), l.getFoodCost(), l.getSalaryCost(),
                l.getTotalCost(), l.getWorkingDays())).toList();
        Totals t = new Totals(p.getRoomTotal(), p.getFoodTotal(), p.getSalaryTotal(), p.getBranchCost(),
                p.getServicesSubtotal().subtract(p.getRoomTotal()).subtract(p.getFoodTotal())
                        .subtract(p.getSalaryTotal()).subtract(p.getBranchCost()).subtract(p.getSrExecCost()),
                p.getSrExecCost(), p.getServicesSubtotal(), p.getOverheadAmount(), p.getProfitAmount(),
                p.getServicesPrice(), p.getRatePerAsset(), p.getSoftwareTotal(), p.getGrossTotal(),
                p.getDiscountAmount(), p.getNetTotal(), p.getGstAmount(), p.getGrandTotal());
        return new ProposalDto(p.getId(), p.getRefNo(), p.getVersion(), p.getProposalDate(), p.getStatus(),
                p.getCustomer().getId(), p.getCustomer().getName(), p.getContactName(), p.getAssetCount(),
                p.getBranchCount(), p.getSrExecDays(), p.getDiscountPercent(), p.getNotes(), p.getParams(),
                p.getTerms(), sw, sv, t, p.getPoNo(), p.getPoDate());
    }

    private static BigDecimal orZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static <T> List<T> orEmpty(List<T> l) {
        return l == null ? List.of() : l;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
