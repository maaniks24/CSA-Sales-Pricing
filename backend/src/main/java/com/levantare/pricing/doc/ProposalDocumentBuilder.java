package com.levantare.pricing.doc;

import com.levantare.pricing.domain.*;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Maps a saved proposal to the values of the Word templates.
 * <ul>
 *   <li>proposals that contain in-scope services use {@code services-proposal.docx} (software lines, if any, are
 *       added to its cost table),</li>
 *   <li>software-only proposals use {@code software-proposal.docx}.</li>
 * </ul>
 */
@Component
public class ProposalDocumentBuilder {

    static final String SERVICES_TEMPLATE = "csa-templates/services-proposal.docx";
    static final String SOFTWARE_TEMPLATE = "csa-templates/software-proposal.docx";

    public byte[] build(Proposal p) throws IOException {
        boolean hasServices = p.getServiceItems().stream().anyMatch(ProposalService::isInScope);
        boolean hasSoftware = !p.getSoftwareItems().isEmpty();
        String template = hasServices ? SERVICES_TEMPLATE : SOFTWARE_TEMPLATE;

        Map<String, String> v = new HashMap<>();
        Customer c = p.getCustomer();
        DocTerms t = p.getTerms();
        v.put("refNo", p.getRefNo());
        v.put("proposalDate", Fmt.date(p.getProposalDate()));
        v.put("customerName", c.getName());
        v.put("customerAddress", c.getAddress() == null ? "" : c.getAddress().replace("\r", "").trim());
        v.put("contactName", p.getContactName() == null || p.getContactName().isBlank() ? "Sir / Madam" : p.getContactName());
        v.put("assetCount", Fmt.inr(p.getAssetCount()));
        v.put("branchCount", String.valueOf(p.getBranchCount()));
        v.put("gstPercent", Fmt.plain(t.getGstPercent()));
        v.put("validityDays", String.valueOf(t.getValidityDays()));
        v.put("commencementDays", String.valueOf(t.getCommencementDays()));
        v.put("pay1", String.valueOf(t.getPay1()));
        v.put("pay2", String.valueOf(t.getPay2()));
        v.put("pay3", String.valueOf(t.getPay3()));
        v.put("trackingTeamSize", String.valueOf(t.getTrackingTeamSize()));
        v.put("dataTeamSize", String.valueOf(t.getDataTeamSize()));
        v.put("advancePercentSoftware", "100");

        List<ProposalService> inScope = p.getServiceItems().stream().filter(ProposalService::isInScope).toList();
        List<Map<String, String>> phases = new ArrayList<>();
        int no = 1;
        for (ProposalService s : inScope) {
            Map<String, String> row = new HashMap<>();
            row.put("no", String.valueOf(no++));
            row.put("activity", s.getService().getActivity() != null ? s.getService().getActivity() : s.getService().getName());
            row.put("assets", Fmt.inr(p.getAssetCount()));
            row.put("days", String.valueOf(s.getWorkingDays()));
            phases.add(row);
        }
        v.put("nextPhase", String.valueOf(no));

        Map<String, List<Map<String, String>>> lists = Map.of("costRows", costRows(p, inScope), "phases", phases);
        Map<String, Boolean> flags = Map.of("hasSoftware", hasSoftware, "hasServices", hasServices);

        try (InputStream in = new ClassPathResource(template).getInputStream()) {
            return DocxTemplateEngine.render(in, v, lists, flags);
        }
    }

    private List<Map<String, String>> costRows(Proposal p, List<ProposalService> inScope) {
        List<Map<String, String>> rows = new ArrayList<>();
        String assets = Fmt.inr(p.getAssetCount());
        if (!inScope.isEmpty()) {
            StringBuilder details = new StringBuilder("Enterprise wide");
            inScope.forEach(s -> details.append("\n- ").append(s.getService().getName()));
            rows.add(row("Services", details.toString(),
                    "Cost of Physical Tracking and Audit for up to " + assets + " assets\n\n"
                            + "Please note that if the number of assets exceeds " + assets
                            + " the cost will increase proportionately.",
                    Fmt.inr(p.getServicesPrice())));
        }
        for (ProposalSoftware l : p.getSoftwareItems()) {
            Software sw = l.getSoftware();
            String covered = Fmt.inr(l.getAssetsCovered());
            String scope = sw.getScopeText() == null ? "" : sw.getScopeText().replace("{assets}", covered);
            rows.add(row("Software", sw.getName(), scope, Fmt.inr(l.getPrice())));
            if (l.getImplementationCharge().signum() > 0) {
                String text = sw.getImplementationText() == null ? "Implementation" : sw.getImplementationText();
                rows.add(row("Software", text, "", Fmt.inr(l.getImplementationCharge())));
            }
        }
        if (p.getDiscountAmount().signum() > 0) {
            rows.add(row("", "Less: Discount @ " + Fmt.plain(p.getDiscountPercent()) + "%", "",
                    "(" + Fmt.inr(p.getDiscountAmount()) + ")"));
        }
        rows.add(row("", "Total (excluding GST)", "", Fmt.inr(p.getNetTotal())));
        rows.add(row("", "GST @ " + Fmt.plain(p.getTerms().getGstPercent()) + "%", "", Fmt.inr(p.getGstAmount())));
        rows.add(row("", "Grand Total (including GST)", "", Fmt.inr(p.getGrandTotal())));
        return rows;
    }

    private static Map<String, String> row(String label, String details, String scope, String amount) {
        return Map.of("label", label, "details", details, "scope", scope, "amount", amount);
    }
}
