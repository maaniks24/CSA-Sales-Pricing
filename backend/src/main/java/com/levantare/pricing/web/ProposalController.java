package com.levantare.pricing.web;

import com.levantare.pricing.service.DashboardService;
import com.levantare.pricing.service.ProposalsService;
import com.levantare.pricing.web.dto.*;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ProposalController {

    static final MediaType DOCX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final ProposalsService proposals;
    private final DashboardService dashboard;

    public ProposalController(ProposalsService proposals, DashboardService dashboard) {
        this.proposals = proposals;
        this.dashboard = dashboard;
    }

    @GetMapping("/dashboard")
    public DashboardDto dashboard() {
        return dashboard.load();
    }

    @GetMapping("/proposals")
    public List<ProposalSummary> list() {
        return proposals.list();
    }

    @GetMapping("/proposals/{id}")
    public ProposalDto get(@PathVariable Long id) {
        return proposals.get(id);
    }

    @PostMapping("/proposals/calculate")
    public ProposalDto calculate(@Valid @RequestBody ProposalRequest req) {
        return proposals.preview(req);
    }

    @PostMapping("/proposals")
    @ResponseStatus(HttpStatus.CREATED)
    public ProposalDto create(@Valid @RequestBody ProposalRequest req) {
        return proposals.create(req);
    }

    @PutMapping("/proposals/{id}")
    public ProposalDto update(@PathVariable Long id, @Valid @RequestBody ProposalRequest req) {
        return proposals.update(id, req);
    }

    @DeleteMapping("/proposals/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        proposals.delete(id);
    }

    @PostMapping("/proposals/{id}/generate-po")
    public ResponseEntity<byte[]> generatePo(@PathVariable Long id) throws IOException {
        var doc = proposals.generatePo(id);
        return ResponseEntity.ok()
                .contentType(DOCX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(doc.fileName()).build().toString())
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .body(doc.content());
    }
}
