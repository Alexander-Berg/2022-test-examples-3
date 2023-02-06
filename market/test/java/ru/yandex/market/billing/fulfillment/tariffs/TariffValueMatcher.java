package ru.yandex.market.billing.fulfillment.tariffs;

import org.hamcrest.Matcher;

import ru.yandex.market.billing.core.order.model.ValueType;
import ru.yandex.market.billing.order.model.BillingUnit;
import ru.yandex.market.mbi.util.MbiMatchers;

/**
 * Матчеры для {@link TariffValue}
 */
public class TariffValueMatcher {
    private TariffValueMatcher() {
    }

    public static Matcher<TariffValue> hasValue(int expectedValue) {
        return MbiMatchers.<TariffValue>newAllOfBuilder()
                .add(TariffValue::getValue, expectedValue, "value")
                .build();
    }

    public static Matcher<TariffValue> hasValueType(ValueType expectedValue) {
        return MbiMatchers.<TariffValue>newAllOfBuilder()
                .add(TariffValue::getValueType, expectedValue, "valueType")
                .build();
    }

    public static Matcher<TariffValue> hasMinValue(Long expectedValue) {
        return MbiMatchers.<TariffValue>newAllOfBuilder()
                .add(TariffValue::getMinValue, expectedValue, "minValue")
                .build();
    }

    public static Matcher<TariffValue> hasMaxValue(Long expectedValue) {
        return MbiMatchers.<TariffValue>newAllOfBuilder()
                .add(TariffValue::getMaxValue, expectedValue, "maxValue")
                .build();
    }

    public static Matcher<TariffValue> hasBillingUnit(BillingUnit expectedValue) {
        return MbiMatchers.<TariffValue>newAllOfBuilder()
                .add(TariffValue::getBillingUnit, expectedValue, "billingUnit")
                .build();
    }
}
