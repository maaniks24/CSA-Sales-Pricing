package com.levantare.pricing.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProposalFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private JsonNode read(String url) throws Exception {
        return json.readTree(mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private long idOf(String list, String code) throws Exception {
        for (JsonNode n : read(list)) if (n.get("code").asText().equals(code)) return n.get("id").asLong();
        throw new AssertionError(code);
    }

    private String servicesBody(long customerId, String softwareJson) throws Exception {
        StringBuilder services = new StringBuilder();
        for (JsonNode s : read("/api/services")) {
            boolean tg = s.get("code").asText().equals("TG");
            if (services.length() > 0) services.append(',');
            services.append("{\"serviceId\":%d,\"inScope\":%b}".formatted(s.get("id").asLong(), !tg));
        }
        return """
            {"customerId":%d,"assetCount":5000,"branchCount":3,"discountPercent":0,
             "terms":{"gstPercent":18,"validityDays":10,"commencementDays":10,"trackingTeamSize":4,"dataTeamSize":1,"pay1":50,"pay2":40,"pay3":10},
             "software":[%s],"services":[%s]}""".formatted(customerId, softwareJson, services);
    }

    @Test
    void servicesProposalFromSaveToWordDocument() throws Exception {
        long apexon = idOf("/api/customers", "C002");
        String body = servicesBody(apexon, "");

        // live calculation, nothing saved
        JsonNode preview = json.readTree(mvc.perform(post("/api/proposals/calculate").contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(preview.at("/totals/servicesPrice").decimalValue()).isEqualByComparingTo("441562.50");
        assertThat(preview.get("id") == null || preview.get("id").isNull()).isTrue();

        // save
        JsonNode saved = json.readTree(mvc.perform(post("/api/proposals").contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        long id = saved.get("id").asLong();
        assertThat(saved.get("status").asText()).isEqualTo("SAVED");
        assertThat(saved.get("refNo").asText()).matches("LTPL/PRO/\\d{4}-\\d{2}/\\d{5}-v1");
        assertThat(saved.get("contactName").asText()).isEqualTo("Prasanna Srinivasa");

        // generate PO
        byte[] docx = mvc.perform(post("/api/proposals/{id}/generate-po", id)).andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("Apexon_")))
                .andReturn().getResponse().getContentAsByteArray();
        Files.write(Path.of("target/generated-services-proposal.docx"), docx);
        String text;
        try (XWPFDocument d = new XWPFDocument(new ByteArrayInputStream(docx));
             XWPFWordExtractor ex = new XWPFWordExtractor(d)) {
            text = ex.getText();
        }
        assertThat(text).doesNotContain("{{").contains("Apexon").contains("Hi Prasanna Srinivasa")
                .contains(saved.get("refNo").asText()).contains("4,41,562.50").contains("Grand Total (including GST)").contains("5,21,043.75")
                .contains("Cost of Physical Tracking and Audit for up to 5,000 assets")
                .doesNotContain("Prabal Motors").doesNotContain("Venugopal");

        JsonNode after = read("/api/proposals/" + id);
        assertThat(after.get("status").asText()).isEqualTo("PO_GENERATED");
        assertThat(after.get("poNo").asText()).matches("LTPL/PO/\\d{4}-\\d{2}/\\d{5}");

        // changing an issued proposal creates version 2
        JsonNode v2 = json.readTree(mvc.perform(put("/api/proposals/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(v2.get("version").asInt()).isEqualTo(2);
        assertThat(v2.get("status").asText()).isEqualTo("SAVED");
        assertThat(v2.get("refNo").asText()).endsWith("-v2");
    }

    @Test
    void softwareOnlyProposalUsesSoftwareTemplate() throws Exception {
        long ud = idOf("/api/customers", "C001");
        long sw = idOf("/api/software", "SAP-CLOUD");
        String body = """
            {"customerId":%d,"assetCount":3000,"discountPercent":10,
             "software":[{"softwareId":%d,"assetsCovered":3000}],"services":[]}""".formatted(ud, sw);

        JsonNode saved = json.readTree(mvc.perform(post("/api/proposals").contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        assertThat(saved.at("/totals/softwareTotal").decimalValue()).isEqualByComparingTo("115000");
        assertThat(saved.at("/totals/grandTotal").decimalValue()).isEqualByComparingTo("122130.00");

        byte[] docx = mvc.perform(post("/api/proposals/{id}/generate-po", saved.get("id").asLong()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        Files.write(Path.of("target/generated-software-proposal.docx"), docx);
        try (XWPFDocument d = new XWPFDocument(new ByteArrayInputStream(docx));
             XWPFWordExtractor ex = new XWPFWordExtractor(d)) {
            assertThat(ex.getText()).doesNotContain("{{").contains("Proposal for Fixed Assets Management Software")
                    .contains("Hi Vijay").contains("UD Trucks India Pvt. Ltd.").contains("90,000").contains("25,000")
                    .contains("Less: Discount @ 10%").contains("(11,500)").contains("1,22,130")
                    .doesNotContain("ICAI").doesNotContain("Four Point");
        }
    }

    @Test
    void rejectsEmptyProposalAndBadPaymentSplit() throws Exception {
        long ud = idOf("/api/customers", "C001");
        mvc.perform(post("/api/proposals").contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":%d,\"software\":[],\"services\":[]}".formatted(ud)))
                .andExpect(status().isConflict());

        String body = servicesBody(ud, "").replace("\"pay3\":10", "\"pay3\":20");
        mvc.perform(post("/api/proposals").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void dashboardAndMasterData() throws Exception {
        JsonNode d = read("/api/dashboard");
        assertThat(d.get("customers").asLong()).isGreaterThanOrEqualTo(2);
        assertThat(d.get("monthly")).hasSize(6);

        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"C900\",\"name\":\"Test Co\",\"assetCount\":10,\"branchCount\":1,\"discountPercent\":2}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"c900\",\"name\":\"Dup\"}")).andExpect(status().isConflict());
        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"\",\"name\":\"\"}")).andExpect(status().isBadRequest());
    }

    @Test
    void proposalCanBeEditedSavedAndRegeneratedRepeatedly() throws Exception {
        long apexon = idOf("/api/customers", "C002");
        String body = servicesBody(apexon, "");
        long id = json.readTree(mvc.perform(post("/api/proposals").contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        // several saves before any document was issued keep version 1
        for (int assets : new int[] {6000, 7000}) {
            JsonNode u = json.readTree(mvc.perform(put("/api/proposals/{id}", id).contentType(MediaType.APPLICATION_JSON)
                    .content(body.replace("\"assetCount\":5000", "\"assetCount\":" + assets)))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
            assertThat(u.get("version").asInt()).isEqualTo(1);
            assertThat(u.get("assetCount").asInt()).isEqualTo(assets);
        }
        assertThat(docText(id)).contains("Approx 7,000 assets").doesNotContain("5,000 assets");

        // after the document was issued every edit creates the next version and the new document shows the change
        JsonNode v2 = json.readTree(mvc.perform(put("/api/proposals/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(body.replace("\"assetCount\":5000", "\"assetCount\":8000").replace("\"discountPercent\":0", "\"discountPercent\":10")))
                .andReturn().getResponse().getContentAsString());
        assertThat(v2.get("version").asInt()).isEqualTo(2);
        String t2 = docText(id);
        assertThat(t2).contains("Approx 8,000 assets").contains("-v2").contains("Less: Discount @ 10%");

        JsonNode v3 = json.readTree(mvc.perform(put("/api/proposals/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(body)).andReturn().getResponse().getContentAsString());
        assertThat(v3.get("version").asInt()).isEqualTo(3);
        assertThat(docText(id)).contains("Approx 5,000 assets").contains("-v3").doesNotContain("Less: Discount");
        // generating again without edits keeps the version
        assertThat(docText(id)).contains("-v3");
        assertThat(read("/api/proposals/" + id).get("version").asInt()).isEqualTo(3);
    }

    private String docText(long id) throws Exception {
        byte[] docx = mvc.perform(post("/api/proposals/{id}/generate-po", id)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        try (XWPFDocument d = new XWPFDocument(new ByteArrayInputStream(docx)); XWPFWordExtractor ex = new XWPFWordExtractor(d)) {
            return ex.getText();
        }
    }

    @Test
    void numberingIsConfigurablePersistedAndEnforced() throws Exception {
        JsonNode orig = read("/api/numbering");
        assertThat(orig.get("proposalPrefix").asText()).isEqualTo("LTPL/PRO");
        long ud = idOf("/api/customers", "C001");
        long sw = idOf("/api/software", "SAP-CLOUD");
        String body = "{\"customerId\":%d,\"software\":[{\"softwareId\":%d}],\"services\":[]}".formatted(ud, sw);
        try {
            String cfg = """
                {"proposalPrefix":"ABC/Q","poPrefix":"ABC/PO","includeFinancialYear":false,"resetEachYear":false,
                 "numberWidth":3,"seriesStartNo":1,"nextProposalNo":998,"nextPoNo":7}""";
            JsonNode saved = json.readTree(mvc.perform(put("/api/numbering").contentType(MediaType.APPLICATION_JSON).content(cfg))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
            assertThat(saved.get("proposalExample").asText()).isEqualTo("ABC/Q/998-v1");
            assertThat(saved.get("poExample").asText()).isEqualTo("ABC/PO/007");
            assertThat(read("/api/numbering").get("nextProposalNo").asLong()).isEqualTo(998);   // persisted

            JsonNode p1 = json.readTree(mvc.perform(post("/api/proposals").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
            assertThat(p1.get("refNo").asText()).isEqualTo("ABC/Q/998-v1");
            JsonNode p2 = json.readTree(mvc.perform(post("/api/proposals").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
            assertThat(p2.get("refNo").asText()).isEqualTo("ABC/Q/999-v1");
            // 3 digits are used up
            mvc.perform(post("/api/proposals").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
            assertThat(read("/api/numbering").get("nextProposalNo").asLong()).isEqualTo(1000);

            mvc.perform(post("/api/proposals/{id}/generate-po", p1.get("id").asLong())).andExpect(status().isOk());
            assertThat(read("/api/proposals/" + p1.get("id").asLong()).get("poNo").asText()).isEqualTo("ABC/PO/007");

            // later prefix changes do not rewrite an issued reference, a re-save only moves the version
            mvc.perform(put("/api/numbering").contentType(MediaType.APPLICATION_JSON)
                    .content(cfg.replace("ABC/Q", "ZZZ").replace("\"numberWidth\":3", "\"numberWidth\":6").replace("998", "1000")))
                    .andExpect(status().isOk());
            JsonNode v2 = json.readTree(mvc.perform(put("/api/proposals/{id}", p1.get("id").asLong()).contentType(MediaType.APPLICATION_JSON)
                    .content(body)).andReturn().getResponse().getContentAsString());
            assertThat(v2.get("refNo").asText()).isEqualTo("ABC/Q/998-v2");

            // invalid input
            mvc.perform(put("/api/numbering").contentType(MediaType.APPLICATION_JSON)
                    .content(cfg.replace("ABC/Q", "bad prefix!"))).andExpect(status().isBadRequest());
            mvc.perform(put("/api/numbering").contentType(MediaType.APPLICATION_JSON)
                    .content(cfg.replace("\"nextPoNo\":7", "\"nextPoNo\":5000"))).andExpect(status().isConflict());
        } finally {
            mvc.perform(put("/api/numbering").contentType(MediaType.APPLICATION_JSON).content("""
                {"proposalPrefix":"LTPL/PRO","poPrefix":"LTPL/PO","includeFinancialYear":true,"resetEachYear":true,
                 "numberWidth":5,"seriesStartNo":1,"nextProposalNo":%d,"nextPoNo":%d}"""
                    .formatted(orig.get("nextProposalNo").asLong(), orig.get("nextPoNo").asLong()))).andExpect(status().isOk());
        }
    }
}
