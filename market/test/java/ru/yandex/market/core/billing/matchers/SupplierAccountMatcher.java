package ru.yandex.market.core.billing.matchers;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hamcrest.Matcher;

import ru.yandex.market.core.supplier.model.SupplierAccount;
import ru.yandex.market.mbi.util.MbiMatchers;

public class SupplierAccountMatcher {

    public static Matcher<SupplierAccount> hasId(long expectedValue) {
        return MbiMatchers.<SupplierAccount>newAllOfBuilder()
                .add(SupplierAccount::getId, expectedValue, "id")
                .build();
    }

    public static Matcher<SupplierAccount> hasSupplierId(long expectedValue) {
        return MbiMatchers.<SupplierAccount>newAllOfBuilder()
                .add(SupplierAccount::getSupplierId, expectedValue, "supplierId")
                .build();
    }

    public static Matcher<SupplierAccount> hasAccountEid(String expectedValue) {
        return MbiMatchers.<SupplierAccount>newAllOfBuilder()
                .add(SupplierAccount::getAccountEid, expectedValue, "accountEid")
                .build();
    }

    public static Matcher<SupplierAccount> hasTotalActSum(BigDecimal expectedValue) {
        return MbiMatchers.<SupplierAccount>newAllOfBuilder()
                .add(SupplierAccount::getTotalActSum, expectedValue, "totalActSum")
                .build();
    }

    public static Matcher<SupplierAccount> hasReceiptSum(BigDecimal expectedValue) {
        return MbiMatchers.<SupplierAccount>newAllOfBuilder()
                .add(SupplierAccount::getReceiptSum, expectedValue, "receiptSum")
                .build();
    }

    public static Matcher<SupplierAccount> hasPaymentDate(LocalDate expectedValue) {
        return MbiMatchers.<SupplierAccount>newAllOfBuilder()
                .add(SupplierAccount::getPaymentDate, expectedValue, "paymentDate")
                .build();
    }
}
