package ru.yandex.market.billing.tasks.reports.distribution.matchers;

import org.hamcrest.Matcher;

import ru.yandex.market.billing.tasks.reports.distribution.cpa.dto.DistributionYtExportDto;
import ru.yandex.market.core.billing.distribution.share.DistributionTariffName;
import ru.yandex.market.mbi.util.MbiMatchers;

/**
 * Матчеры для {@link DistributionYtExportDto}
 */
public class DistributionYtExportDtoMatcher {

    private DistributionYtExportDtoMatcher() {
    }

    public static Matcher<DistributionYtExportDto> hasOrderId(Long expectedValue) {
        return MbiMatchers.<DistributionYtExportDto>newAllOfBuilder()
                .add(DistributionYtExportDto::getOrderId, expectedValue, "orderId")
                .build();
    }

    public static Matcher<DistributionYtExportDto> hasItemId(Long expectedValue) {
        return MbiMatchers.<DistributionYtExportDto>newAllOfBuilder()
                .add(DistributionYtExportDto::getItemId, expectedValue, "itemId")
                .build();
    }

    public static Matcher<DistributionYtExportDto> hasStatus(Integer expectedValue) {
        return MbiMatchers.<DistributionYtExportDto>newAllOfBuilder()
                .add(DistributionYtExportDto::getStatus, expectedValue, "status")
                .build();
    }

    public static Matcher<DistributionYtExportDto> hasClid(Long expectedValue) {
        return MbiMatchers.<DistributionYtExportDto>newAllOfBuilder()
                .add(DistributionYtExportDto::getClid, expectedValue, "clid")
                .build();
    }

    public static Matcher<DistributionYtExportDto> hasVid(String expectedValue) {
        return MbiMatchers.<DistributionYtExportDto>newAllOfBuilder()
                .add(DistributionYtExportDto::getVid, expectedValue, "vid")
                .build();
    }

    public static Matcher<DistributionYtExportDto> hasDateCreated(String expectedValue) {
        return MbiMatchers.<DistributionYtExportDto>newAllOfBuilder()
                .add(DistributionYtExportDto::getDateCreated, expectedValue, "dateCreated")
                .build();
    }

    public static Matcher<DistributionYtExportDto> hasDateUpdated(String expectedValue) {
        return MbiMatchers.<DistributionYtExportDto>newAllOfBuilder()
                .add(DistributionYtExportDto::getDateUpdated, expectedValue, "dateUpdated")
                .build();
    }

    public static Matcher<DistributionYtExportDto> hasItemCount(Integer expectedValue) {
        return MbiMatchers.<DistributionYtExportDto>newAllOfBuilder()
                .add(DistributionYtExportDto::getItemCount, expectedValue, "itemCount")
                .build();
    }

    public static Matcher<DistributionYtExportDto> hasCart(String expectedValue) {
        return MbiMatchers.<DistributionYtExportDto>newAllOfBuilder()
                .add(DistributionYtExportDto::getCart, expectedValue, "cart")
                .build();
    }

    public static Matcher<DistributionYtExportDto> hasPayment(String expectedValue) {
        return MbiMatchers.<DistributionYtExportDto>newAllOfBuilder()
                .add(DistributionYtExportDto::getPayment, expectedValue, "payment")
                .build();
    }

    public static Matcher<DistributionYtExportDto> hasTariff(String expectedValue) {
        return MbiMatchers.<DistributionYtExportDto>newAllOfBuilder()
                .add(DistributionYtExportDto::getTariff, expectedValue, "tariff")
                .build();
    }

    public static Matcher<DistributionYtExportDto> hasTariffName(DistributionTariffName expectedValue) {
        return MbiMatchers.<DistributionYtExportDto>newAllOfBuilder()
            .add(DistributionYtExportDto::getTariffName, expectedValue.toString(), "tariffName")
            .build();
    }

    public static Matcher<DistributionYtExportDto> hasTariffRate(String expectedValue) {
        return MbiMatchers.<DistributionYtExportDto>newAllOfBuilder()
                .add(DistributionYtExportDto::getTariffRate, expectedValue, "tariffRate")
                .build();
    }

    public static Matcher<DistributionYtExportDto> hasAdditionalInfoStr(String expectedValue) {
        return MbiMatchers.<DistributionYtExportDto>newAllOfBuilder()
                .add(DistributionYtExportDto::getAdditionalInfoStr, expectedValue, "additionalInfoStr")
                .build();
    }

    public static Matcher<DistributionYtExportDto> hasAdditionalInfoBitmask(int expectedValue) {
        return MbiMatchers.<DistributionYtExportDto>newAllOfBuilder()
                .add(DistributionYtExportDto::getAdditionalInfoBitmask, expectedValue, "additionalInfoBitMask")
                .build();
    }
}
