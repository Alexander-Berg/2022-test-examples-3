package ru.yandex.market.billing.distribution.share;

import java.math.BigDecimal;

import org.hamcrest.Matcher;

import ru.yandex.market.core.billing.distribution.share.DistributionTariffName;
import ru.yandex.market.mbi.util.MbiMatchers;

/**
 * Матчер для {@link DistributionTariffRateAndName}
 */
public class DistributionTariffRateAndNameMatcher {
    public static Matcher<DistributionTariffRateAndName> hasTariffRate(BigDecimal expectedValue) {
        return MbiMatchers.<DistributionTariffRateAndName>newAllOfBuilder()
            .add(DistributionTariffRateAndName::getTariffRate, expectedValue, "tariffRate")
            .build();
    }

    public static Matcher<DistributionTariffRateAndName> hasTariffName(DistributionTariffName expectedValue) {
        return MbiMatchers.<DistributionTariffRateAndName>newAllOfBuilder()
            .add(DistributionTariffRateAndName::getTariffName, expectedValue, "tariffName")
            .build();
    }
}
