package ru.yandex.market.core.sorting.matchers;

import org.hamcrest.Matcher;

import ru.yandex.market.core.sorting.model.SortingDailyTariff;
import ru.yandex.market.mbi.util.MbiMatchers;

public class SortingDailyTariffMatcher {

    public static Matcher<SortingDailyTariff> hasSupplierId(Long expectedValue) {
        return MbiMatchers.<SortingDailyTariff>newAllOfBuilder()
                .add(SortingDailyTariff::getSupplierId, expectedValue, "supplierId")
                .build();
    }

    public static Matcher<SortingDailyTariff> hasValue(Long expectedValue) {
        return MbiMatchers.<SortingDailyTariff>newAllOfBuilder()
                .add(SortingDailyTariff::getValue, expectedValue, "value")
                .build();
    }
}
