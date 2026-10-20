package com.levantare.pricing.calc;

import static org.assertj.core.api.Assertions.assertThat;

import com.levantare.pricing.calc.PricingCalculator.*;
import com.levantare.pricing.domain.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Reproduces the numbers of the sample workbook (5000 assets, 3 branches, TG out of scope). */
class PricingCalculatorTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    static PricingParams excelParams() {
        PricingParams p = new PricingParams();
        p.setTrackingRoom(bd("1000")); p.setTrackingFood(bd("500")); p.setTrackingSalary(bd("1000"));
        p.setDataAuditSalary(bd("1000"));
        p.setSrExecRoom(bd("2000")); p.setSrExecFood(bd("1000")); p.setSrExecSalary(bd("2500"));
        p.setCostPerBranch(bd("5000")); p.setTravelCost(bd("25000"));
        p.setOverheadPercent(bd("50")); p.setProfitPercent(bd("50"));
        return p;
    }

    static List<ServiceIn> excelServices(boolean tagging) {
        return List.of(
                new ServiceIn(true, bd("800"), ResourceType.DATA_AUDIT, false, false),   // FAR
                new ServiceIn(true, bd("150"), ResourceType.TRACKING, true, true),       // PV
                new ServiceIn(true, bd("500"), ResourceType.DATA_AUDIT, false, false),   // DC
                new ServiceIn(true, bd("750"), ResourceType.DATA_AUDIT, false, false),   // DM
                new ServiceIn(true, bd("100"), ResourceType.TRACKING, false, false),     // DR
                new ServiceIn(tagging, bd("70"), ResourceType.TRACKING, true, true));    // TG
    }

    @Test
    void matchesWorkbook() {
        DocTerms terms = new DocTerms();
        terms.setTrackingTeamSize(4);
        Result r = PricingCalculator.calculate(new Input(5000, 3, BigDecimal.ZERO, BigDecimal.ZERO, excelParams(),
                terms, excelServices(false), List.of()));

        assertThat(r.services().get(0).effortDays()).isEqualByComparingTo("6.25");
        assertThat(r.services().get(1).effortDays()).isEqualByComparingTo("33.3333");
        assertThat(r.services().get(1).total()).isEqualByComparingTo("83333.33");
        assertThat(r.services().get(5).total()).isEqualByComparingTo("0");   // TG scope = No
        assertThat(r.roomTotal()).isEqualByComparingTo("33333.33");
        assertThat(r.foodTotal()).isEqualByComparingTo("16666.67");
        assertThat(r.salaryTotal()).isEqualByComparingTo("106250.00");
        assertThat(r.branchCost()).isEqualByComparingTo("15000");
        assertThat(r.travelCost()).isEqualByComparingTo("25000");
        assertThat(r.servicesSubtotal()).isEqualByComparingTo("196250.00");
        assertThat(r.overheadAmount()).isEqualByComparingTo("98125.00");
        assertThat(r.profitAmount()).isEqualByComparingTo("147187.50");
        assertThat(r.servicesPrice()).isEqualByComparingTo("441562.50");
        assertThat(r.ratePerAsset()).isEqualByComparingTo("88.3125");
        assertThat(r.services().get(1).workingDays()).isEqualTo(9);          // ceil(33.33 / 4 people)
    }

    @Test
    void softwareScalesWithAssetsAndDiscountAndGstApply() {
        DocTerms terms = new DocTerms();
        // 90,000 per 3,000 assets, 6,000 assets -> 180,000 + 25,000 implementation
        Result r = PricingCalculator.calculate(new Input(6000, 1, BigDecimal.ZERO, bd("10"), excelParams(), terms,
                List.of(), List.of(new SoftwareIn(bd("90000"), 3000, bd("25000"), 6000))));

        assertThat(r.servicesPrice()).isEqualByComparingTo("0");   // no service => no branch / travel cost
        assertThat(r.softwareTotal()).isEqualByComparingTo("205000");
        assertThat(r.discountAmount()).isEqualByComparingTo("20500");
        assertThat(r.netTotal()).isEqualByComparingTo("184500");
        assertThat(r.gstAmount()).isEqualByComparingTo("33210");
        assertThat(r.grandTotal()).isEqualByComparingTo("217710");
    }

    @Test
    void seniorExecutiveDaysAddToServicesCost() {
        Result base = PricingCalculator.calculate(new Input(5000, 3, BigDecimal.ZERO, BigDecimal.ZERO, excelParams(),
                new DocTerms(), excelServices(false), List.of()));
        Result withSr = PricingCalculator.calculate(new Input(5000, 3, bd("2"), BigDecimal.ZERO, excelParams(),
                new DocTerms(), excelServices(false), List.of()));
        // 2 days * (2000 + 1000 + 2500) = 11,000 before loadings, x1.5 x1.5
        assertThat(withSr.servicesPrice().subtract(base.servicesPrice())).isEqualByComparingTo("24750.00");
    }
}
