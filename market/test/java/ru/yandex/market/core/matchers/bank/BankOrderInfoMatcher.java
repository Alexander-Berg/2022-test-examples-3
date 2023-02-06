package ru.yandex.market.core.matchers.bank;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hamcrest.Matcher;

import ru.yandex.market.core.order.payment.BankOrderInfo;
import ru.yandex.market.core.order.payment.TransactionType;
import ru.yandex.market.mbi.util.MbiMatchers;

public class BankOrderInfoMatcher {

    public static Matcher<BankOrderInfo> hasTrantime(LocalDate expectedValue) {
        return MbiMatchers.<BankOrderInfo>newAllOfBuilder()
                .add(BankOrderInfo::getTrantime, expectedValue, "trantime")
                .build();
    }

    public static Matcher<BankOrderInfo> hasBankOrderDate(LocalDate expectedValue) {
        return MbiMatchers.<BankOrderInfo>newAllOfBuilder()
                .add(BankOrderInfo::getBankOrderDate, expectedValue, "bankOrderDate")
                .build();
    }

    public static Matcher<BankOrderInfo> hasCreationDate(LocalDate expectedValue) {
        return MbiMatchers.<BankOrderInfo>newAllOfBuilder()
                .add(BankOrderInfo::getCreationDate, expectedValue, "creationDate")
                .build();
    }

    public static Matcher<BankOrderInfo> hasBankSum(BigDecimal expectedValue) {
        return MbiMatchers.<BankOrderInfo>newAllOfBuilder()
                .add(BankOrderInfo::getBankSum, expectedValue, "bankSum")
                .build();
    }

    public static Matcher<BankOrderInfo> hasItemSum(BigDecimal expectedValue) {
        return MbiMatchers.<BankOrderInfo>newAllOfBuilder()
                .add(BankOrderInfo::getItemSum, expectedValue, "itemSum")
                .build();
    }

    public static Matcher<BankOrderInfo> hasTransactionType(TransactionType expectedValue) {
        return MbiMatchers.<BankOrderInfo>newAllOfBuilder()
                .add(BankOrderInfo::getTransactionType, expectedValue, "transactionType")
                .build();
    }

    public static Matcher<BankOrderInfo> hasTrustId(String expectedValue) {
        return MbiMatchers.<BankOrderInfo>newAllOfBuilder()
                .add(BankOrderInfo::getTrustId, expectedValue, "trustId")
                .build();
    }

    public static Matcher<BankOrderInfo> hasShopSku(String expectedValue) {
        return MbiMatchers.<BankOrderInfo>newAllOfBuilder()
                .add(BankOrderInfo::getShopSku, expectedValue, "shopSku")
                .build();
    }

    public static Matcher<BankOrderInfo> hasOfferName(String expectedValue) {
        return MbiMatchers.<BankOrderInfo>newAllOfBuilder()
                .add(BankOrderInfo::getOfferName, expectedValue, "offerName")
                .build();
    }

    public static Matcher<BankOrderInfo> hasItemCount(Long expectedValue) {
        return MbiMatchers.<BankOrderInfo>newAllOfBuilder()
                .add(BankOrderInfo::getItemCount, expectedValue, "itemCount")
                .build();
    }

    public static Matcher<BankOrderInfo> hasServiceOrderId(String expectedValue) {
        return MbiMatchers.<BankOrderInfo>newAllOfBuilder()
                .add(BankOrderInfo::getServiceOrderId, expectedValue, "serviceOrderId")
                .build();
    }

    public static Matcher<BankOrderInfo> hasOrderId(Long expectedValue) {
        return MbiMatchers.<BankOrderInfo>newAllOfBuilder()
                .add(BankOrderInfo::getOrderId, expectedValue, "orderId")
                .build();
    }

    public static Matcher<BankOrderInfo> hasPaymentType(String expectedValue) {
        return MbiMatchers.<BankOrderInfo>newAllOfBuilder()
                .add(BankOrderInfo::getPaymentType, expectedValue, "paymentType")
                .build();
    }

    public static Matcher<BankOrderInfo> hasBankOrderId(Long expectedValue) {
        return MbiMatchers.<BankOrderInfo>newAllOfBuilder()
                .add(BankOrderInfo::getBankOrderId, expectedValue, "bankOrderId")
                .build();
    }
}
