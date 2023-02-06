package ru.yandex.market.core.agency.matchers;

import java.time.Instant;

import org.hamcrest.Matcher;

import ru.yandex.market.core.billing.model.AgencyCommissionBilledAmount;
import ru.yandex.market.mbi.util.MbiMatchers;

public class AgencyCommissionBilledAmountMatcher {

    public static Matcher<AgencyCommissionBilledAmount> hasSupplierId(Long expectedValue) {
        return MbiMatchers.<AgencyCommissionBilledAmount>newAllOfBuilder()
                .add(AgencyCommissionBilledAmount::getPartnerId, expectedValue, "supplierId")
                .build();
    }

    public static Matcher<AgencyCommissionBilledAmount> hasOrderId(Long expectedValue) {
        return MbiMatchers.<AgencyCommissionBilledAmount>newAllOfBuilder()
                .add(AgencyCommissionBilledAmount::getOrderId, expectedValue, "orderId")
                .build();
    }

    public static Matcher<AgencyCommissionBilledAmount> hasItemId(Long expectedValue) {
        return MbiMatchers.<AgencyCommissionBilledAmount>newAllOfBuilder()
                .add(AgencyCommissionBilledAmount::getItemId, expectedValue, "itemId")
                .build();
    }

    public static Matcher<AgencyCommissionBilledAmount> hasPaymentId(Long expectedValue) {
        return MbiMatchers.<AgencyCommissionBilledAmount>newAllOfBuilder()
                .add(AgencyCommissionBilledAmount::getPaymentId, expectedValue, "paymentId")
                .build();
    }

    public static Matcher<AgencyCommissionBilledAmount> hasTrustPaymentId(String expectedValue) {
        return MbiMatchers.<AgencyCommissionBilledAmount>newAllOfBuilder()
                .add(AgencyCommissionBilledAmount::getTrustPaymentId, expectedValue, "trustPaymentId")
                .build();
    }

    public static Matcher<AgencyCommissionBilledAmount> hasTrantime(Instant expectedValue) {
        return MbiMatchers.<AgencyCommissionBilledAmount>newAllOfBuilder()
                .add(AgencyCommissionBilledAmount::getTrantime, expectedValue, "trantime")
                .build();
    }

    public static Matcher<AgencyCommissionBilledAmount> hasPaymentAmount(Long expectedValue) {
        return MbiMatchers.<AgencyCommissionBilledAmount>newAllOfBuilder()
                .add(AgencyCommissionBilledAmount::getPaymentAmount, expectedValue, "paymentAmount")
                .build();
    }

    public static Matcher<AgencyCommissionBilledAmount> hasTariffValue(Long expectedValue) {
        return MbiMatchers.<AgencyCommissionBilledAmount>newAllOfBuilder()
                .add(AgencyCommissionBilledAmount::getTariffValue, expectedValue, "tariffValue")
                .build();
    }

    public static Matcher<AgencyCommissionBilledAmount> hasAmount(Long expectedValue) {
        return MbiMatchers.<AgencyCommissionBilledAmount>newAllOfBuilder()
                .add(AgencyCommissionBilledAmount::getAmount, expectedValue, "amount")
                .build();
    }
}
