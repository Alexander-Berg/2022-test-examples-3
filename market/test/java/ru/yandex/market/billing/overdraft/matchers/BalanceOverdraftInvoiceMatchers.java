package ru.yandex.market.billing.overdraft.matchers;

import java.time.Instant;
import java.time.LocalDate;

import org.hamcrest.Matcher;

import ru.yandex.market.billing.overdraft.imprt.BalanceInvoiceStatus;
import ru.yandex.market.billing.overdraft.imprt.BalanceOverdraftInvoice;
import ru.yandex.market.mbi.util.MbiMatchers;

import static org.hamcrest.CoreMatchers.allOf;

/**
 * @author vbudnev
 */
public class BalanceOverdraftInvoiceMatchers {

    public static Matcher<BalanceOverdraftInvoice> hasClientId(Long expectedValue) {
        return MbiMatchers.<BalanceOverdraftInvoice>newAllOfBuilder()
                .add(BalanceOverdraftInvoice::getClientId, expectedValue, "clientId")
                .build();
    }

    public static Matcher<BalanceOverdraftInvoice> hasExternalId(String expectedValue) {
        return MbiMatchers.<BalanceOverdraftInvoice>newAllOfBuilder()
                .add(BalanceOverdraftInvoice::getExternalId, expectedValue, "externalId")
                .build();
    }

    public static Matcher<BalanceOverdraftInvoice> hasInvoiceCreationTime(Instant expectedValue) {
        return MbiMatchers.<BalanceOverdraftInvoice>newAllOfBuilder()
                .add(BalanceOverdraftInvoice::getInvoiceCreationTime, expectedValue, "paymentCreationTime")
                .build();
    }

    public static Matcher<BalanceOverdraftInvoice> hasPaymentDeadlineDate(LocalDate expectedValue) {
        return MbiMatchers.<BalanceOverdraftInvoice>newAllOfBuilder()
                .add(BalanceOverdraftInvoice::getPaymentDeadlineDate, expectedValue, "paymentDeadlineDate")
                .build();
    }


    public static Matcher<BalanceOverdraftInvoice> hasInvoicePaymentTime(Instant expectedValue) {
        return MbiMatchers.<BalanceOverdraftInvoice>newAllOfBuilder()
                .add(BalanceOverdraftInvoice::getInvoicePaymentTime, expectedValue, "receiptDt")
                .build();
    }

    public static Matcher<BalanceOverdraftInvoice> hasStatus(BalanceInvoiceStatus expectedValue) {
        return MbiMatchers.<BalanceOverdraftInvoice>newAllOfBuilder()
                .add(BalanceOverdraftInvoice::getStatus, expectedValue, "status")
                .build();
    }

    public static Matcher<BalanceOverdraftInvoice> invoiceMatcher(
            Long clientId,
            String eid,
            Instant paymentCreationTime,
            LocalDate paymentDeadlineTime,
            Instant receiptDt,
            BalanceInvoiceStatus status
    ) {
        return allOf(
                hasClientId(clientId),
                hasExternalId(eid),
                hasInvoiceCreationTime(paymentCreationTime),
                hasPaymentDeadlineDate(paymentDeadlineTime),
                hasInvoicePaymentTime(receiptDt),
                hasStatus(status)
        );
    }

}
