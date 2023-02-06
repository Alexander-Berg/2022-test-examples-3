package ru.yandex.market.core.sorting.matchers;

import org.hamcrest.Matcher;

import ru.yandex.market.core.fulfillment.model.BillingServiceType;
import ru.yandex.market.core.sorting.model.SortingIntakeType;
import ru.yandex.market.core.sorting.model.SortingOrderTariff;
import ru.yandex.market.mbi.util.MbiMatchers;

public class SortingOrderTariffMatcher {

    public static Matcher<SortingOrderTariff> hasSupplierId(Long expectedValue) {
        return MbiMatchers.<SortingOrderTariff>newAllOfBuilder()
                .add(SortingOrderTariff::getSupplierId, expectedValue, "supplierId")
                .build();
    }

    public static Matcher<SortingOrderTariff> hasServiceType(BillingServiceType expectedValue) {
        return MbiMatchers.<SortingOrderTariff>newAllOfBuilder()
                .add(SortingOrderTariff::getServiceType, expectedValue, "serviceType")
                .build();
    }

    public static Matcher<SortingOrderTariff> hasIntakeType(SortingIntakeType expectedValue) {
        return MbiMatchers.<SortingOrderTariff>newAllOfBuilder()
                .add(SortingOrderTariff::getIntakeType, expectedValue, "intakeType")
                .build();
    }

    public static Matcher<SortingOrderTariff> hasValue(Long expectedValue) {
        return MbiMatchers.<SortingOrderTariff>newAllOfBuilder()
                .add(SortingOrderTariff::getValue, expectedValue, "value")
                .build();
    }

}
