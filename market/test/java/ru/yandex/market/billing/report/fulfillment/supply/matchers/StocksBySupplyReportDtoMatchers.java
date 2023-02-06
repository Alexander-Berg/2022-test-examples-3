package ru.yandex.market.billing.report.fulfillment.supply.matchers;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hamcrest.Matcher;

import ru.yandex.market.billing.report.fulfillment.supply.model.StocksBySupplyReportDto;
import ru.yandex.market.mbi.util.MbiMatchers;

public class StocksBySupplyReportDtoMatchers {

    private StocksBySupplyReportDtoMatchers() {
    }

    public static Matcher<StocksBySupplyReportDto> hasSupplierId(Long expectedValue) {
        return MbiMatchers.<StocksBySupplyReportDto>newAllOfBuilder()
                .add(StocksBySupplyReportDto::getSupplierId, expectedValue, "supplierId")
                .build();
    }

    public static Matcher<StocksBySupplyReportDto> hasSupplyId(Long expectedValue) {
        return MbiMatchers.<StocksBySupplyReportDto>newAllOfBuilder()
                .add(StocksBySupplyReportDto::getSupplyId, expectedValue, "supplyId")
                .build();
    }

    public static Matcher<StocksBySupplyReportDto> hasShopSku(String expectedValue) {
        return MbiMatchers.<StocksBySupplyReportDto>newAllOfBuilder()
                .add(StocksBySupplyReportDto::getShopSku, expectedValue, "shopSku")
                .build();
    }

    public static Matcher<StocksBySupplyReportDto> hasBillingTimestamp(LocalDate expectedValue) {
        return MbiMatchers.<StocksBySupplyReportDto>newAllOfBuilder()
                .add(StocksBySupplyReportDto::getBillingDate, expectedValue, "billingTimestamp")
                .build();
    }

    public static Matcher<StocksBySupplyReportDto> hasWeight(BigDecimal expectedValue) {
        return MbiMatchers.<StocksBySupplyReportDto>newAllOfBuilder()
                .add(StocksBySupplyReportDto::getWeight, expectedValue, "weight")
                .build();
    }
}
