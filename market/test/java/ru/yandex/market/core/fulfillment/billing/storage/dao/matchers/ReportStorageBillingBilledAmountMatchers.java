package ru.yandex.market.core.fulfillment.billing.storage.dao.matchers;

import java.time.LocalDate;

import org.hamcrest.Matcher;

import ru.yandex.market.core.fulfillment.billing.storage.model.ReportStorageBillingBilledAmount;
import ru.yandex.market.core.fulfillment.model.FulfillmentOperationType;
import ru.yandex.market.mbi.util.MbiMatchers;

public final class ReportStorageBillingBilledAmountMatchers {

    public static Matcher<ReportStorageBillingBilledAmount> hasSupplierId(long expectedValue) {
        return MbiMatchers.<ReportStorageBillingBilledAmount>newAllOfBuilder()
                .add(reportBilled -> reportBilled.getBilledAmount().getSupplierId(), expectedValue, "supplierId")
                .build();
    }

    public static Matcher<ReportStorageBillingBilledAmount> hasSupplyId(long expectedValue) {
        return MbiMatchers.<ReportStorageBillingBilledAmount>newAllOfBuilder()
                .add(reportBilled -> reportBilled.getBilledAmount().getSupplyId(), expectedValue, "supplyId")
                .build();
    }

    public static Matcher<ReportStorageBillingBilledAmount> hasOperationType(FulfillmentOperationType expectedValue) {
        return MbiMatchers.<ReportStorageBillingBilledAmount>newAllOfBuilder()
                .add(ReportStorageBillingBilledAmount::getOperationType, expectedValue, "operationType")
                .build();
    }

    public static Matcher<ReportStorageBillingBilledAmount> hasShopSku(String expectedValue) {
        return MbiMatchers.<ReportStorageBillingBilledAmount>newAllOfBuilder()
                .add(reportBilled -> reportBilled.getBilledAmount().getShopSku(), expectedValue, "shopSku")
                .build();
    }

    public static Matcher<ReportStorageBillingBilledAmount> hasStartTimestamp(LocalDate expectedValue) {
        return MbiMatchers.<ReportStorageBillingBilledAmount>newAllOfBuilder()
                .add(reportBilled -> reportBilled.getBilledAmount().getStartDate(), expectedValue, "startDate")
                .build();
    }

    public static Matcher<ReportStorageBillingBilledAmount> hasDaysOfPaidStorage(Integer expectedValue) {
        return MbiMatchers.<ReportStorageBillingBilledAmount>newAllOfBuilder()
                .add(reportBilled -> reportBilled.getDaysOfPaidStorage(), expectedValue, "daysOfPaidStorage")
                .build();
    }

    public static Matcher<ReportStorageBillingBilledAmount> hasDaysToPay(Integer expectedValue) {
        return MbiMatchers.<ReportStorageBillingBilledAmount>newAllOfBuilder()
                .add(reportBilled -> reportBilled.getDaysToPay(), expectedValue, "daysToPay")
                .build();
    }

    public static Matcher<ReportStorageBillingBilledAmount> hasDaysOnStock(Integer expectedValue) {
        return MbiMatchers.<ReportStorageBillingBilledAmount>newAllOfBuilder()
                .add(reportBilled -> reportBilled.getDaysOnStock(), expectedValue, "daysOnStock")
                .build();
    }
}
