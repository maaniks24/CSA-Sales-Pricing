package com.levantare.pricing.calc;

import com.levantare.pricing.domain.DocTerms;
import com.levantare.pricing.domain.PricingParams;
import com.levantare.pricing.domain.ResourceType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure pricing logic. It mirrors the "Template for Apps" sheet of the sample workbook:
 * <pre>
 *   effort days   = assets / PPD-PP                       (only for services in scope)
 *   room / food   = effort days * rate                    (only where the phase includes them)
 *   salary        = effort days * salary rate of the phase's resource type
 *   sub-total     = sum of phases + (branches * cost per branch) + travel (+ senior executive days)
 *   price         = sub-total * (1 + overhead%) * (1 + profit%)
 * </pre>
 * Software is priced proportionately to the covered asset count; the discount applies to
 * software + services, GST on the discounted amount.
 */
public final class PricingCalculator {

    private static final int SCALE = 10;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private PricingCalculator() {}

    public record ServiceIn(boolean inScope, BigDecimal assetsPerDay, ResourceType resourceType,
                            boolean includeRoom, boolean includeFood) {}

    public record SoftwareIn(BigDecimal price, int assetBlock, BigDecimal implementationCharge, int assetsCovered) {}

    public record Input(int assets, int branches, BigDecimal srExecDays, BigDecimal discountPercent,
                        PricingParams params, DocTerms terms,
                        List<ServiceIn> services, List<SoftwareIn> software) {}

    public record ServiceOut(BigDecimal effortDays, BigDecimal room, BigDecimal food, BigDecimal salary,
                             BigDecimal total, int workingDays) {}

    public record SoftwareOut(BigDecimal price, BigDecimal implementationCharge) {}

    public record Result(List<ServiceOut> services, List<SoftwareOut> software,
                         BigDecimal roomTotal, BigDecimal foodTotal, BigDecimal salaryTotal,
                         BigDecimal branchCost, BigDecimal travelCost, BigDecimal srExecCost,
                         BigDecimal servicesSubtotal, BigDecimal overheadAmount, BigDecimal profitAmount,
                         BigDecimal servicesPrice, BigDecimal ratePerAsset,
                         BigDecimal softwareTotal, BigDecimal grossTotal, BigDecimal discountAmount,
                         BigDecimal netTotal, BigDecimal gstAmount, BigDecimal grandTotal) {}

    public static Result calculate(Input in) {
        PricingParams p = in.params();
        List<ServiceOut> serviceOut = new ArrayList<>();
        BigDecimal room = BigDecimal.ZERO, food = BigDecimal.ZERO, salary = BigDecimal.ZERO;
        boolean anyService = false;

        for (ServiceIn s : in.services()) {
            if (!s.inScope() || s.assetsPerDay() == null || s.assetsPerDay().signum() <= 0) {
                serviceOut.add(new ServiceOut(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, 0));
                continue;
            }
            anyService = true;
            BigDecimal effort = BigDecimal.valueOf(in.assets()).divide(s.assetsPerDay(), SCALE, RoundingMode.HALF_UP);
            BigDecimal r = s.includeRoom() ? effort.multiply(roomRate(p, s.resourceType())) : BigDecimal.ZERO;
            BigDecimal f = s.includeFood() ? effort.multiply(foodRate(p, s.resourceType())) : BigDecimal.ZERO;
            BigDecimal sal = effort.multiply(salaryRate(p, s.resourceType()));
            room = room.add(r);
            food = food.add(f);
            salary = salary.add(sal);
            int team = s.resourceType() == ResourceType.DATA_AUDIT ? in.terms().getDataTeamSize()
                    : in.terms().getTrackingTeamSize();
            int days = effort.divide(BigDecimal.valueOf(Math.max(team, 1)), 0, RoundingMode.CEILING).intValue();
            serviceOut.add(new ServiceOut(effort.setScale(4, RoundingMode.HALF_UP), money(r), money(f), money(sal),
                    money(r.add(f).add(sal)), days));
        }

        // Branch cost, travel and senior executive time only make sense when there is a service to deliver.
        BigDecimal branchCost = anyService ? p.getCostPerBranch().multiply(BigDecimal.valueOf(in.branches())) : BigDecimal.ZERO;
        BigDecimal travel = anyService ? p.getTravelCost() : BigDecimal.ZERO;
        BigDecimal srDays = in.srExecDays() == null ? BigDecimal.ZERO : in.srExecDays();
        BigDecimal srExec = anyService
                ? srDays.multiply(p.getSrExecRoom().add(p.getSrExecFood()).add(p.getSrExecSalary()))
                : BigDecimal.ZERO;

        BigDecimal subtotal = room.add(food).add(salary).add(branchCost).add(travel).add(srExec);
        BigDecimal overhead = percent(subtotal, p.getOverheadPercent());
        BigDecimal afterOverhead = subtotal.add(overhead);
        BigDecimal profit = percent(afterOverhead, p.getProfitPercent());
        BigDecimal servicesPrice = afterOverhead.add(profit);
        BigDecimal ratePerAsset = in.assets() > 0
                ? servicesPrice.divide(BigDecimal.valueOf(in.assets()), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        List<SoftwareOut> softwareOut = new ArrayList<>();
        BigDecimal softwareTotal = BigDecimal.ZERO;
        for (SoftwareIn sw : in.software()) {
            BigDecimal price = sw.price()
                    .multiply(BigDecimal.valueOf(sw.assetsCovered()))
                    .divide(BigDecimal.valueOf(Math.max(sw.assetBlock(), 1)), SCALE, RoundingMode.HALF_UP);
            BigDecimal impl = sw.implementationCharge();
            softwareOut.add(new SoftwareOut(money(price), money(impl)));
            softwareTotal = softwareTotal.add(money(price)).add(money(impl));
        }

        BigDecimal servicesPriceR = money(servicesPrice);
        BigDecimal gross = servicesPriceR.add(softwareTotal);
        BigDecimal discount = money(percent(gross, in.discountPercent()));
        BigDecimal net = gross.subtract(discount);
        BigDecimal gst = money(percent(net, in.terms().getGstPercent()));

        return new Result(serviceOut, softwareOut,
                money(room), money(food), money(salary), money(branchCost), money(travel), money(srExec),
                money(subtotal), money(overhead), money(profit), servicesPriceR, ratePerAsset,
                softwareTotal, gross, discount, net, gst, net.add(gst));
    }

    private static BigDecimal percent(BigDecimal base, BigDecimal pct) {
        return base.multiply(pct == null ? BigDecimal.ZERO : pct).divide(HUNDRED, SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal roomRate(PricingParams p, ResourceType t) {
        return t == ResourceType.SR_EXECUTIVE ? p.getSrExecRoom() : p.getTrackingRoom();
    }

    private static BigDecimal foodRate(PricingParams p, ResourceType t) {
        return t == ResourceType.SR_EXECUTIVE ? p.getSrExecFood() : p.getTrackingFood();
    }

    private static BigDecimal salaryRate(PricingParams p, ResourceType t) {
        return switch (t) {
            case TRACKING -> p.getTrackingSalary();
            case DATA_AUDIT -> p.getDataAuditSalary();
            case SR_EXECUTIVE -> p.getSrExecSalary();
        };
    }
}
