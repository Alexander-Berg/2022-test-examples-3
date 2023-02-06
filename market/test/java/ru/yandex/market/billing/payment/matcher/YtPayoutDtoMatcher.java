package ru.yandex.market.billing.payment.matcher;

import java.time.Instant;

import org.hamcrest.Matcher;

import ru.yandex.market.billing.core.OperatingUnit;
import ru.yandex.market.billing.payment.model.YtPayoutDto;
import ru.yandex.market.core.payment.EntityType;
import ru.yandex.market.core.payment.PaymentOrderCurrency;
import ru.yandex.market.core.payment.PayoutProductType;
import ru.yandex.market.core.payment.PaysysTypeCc;
import ru.yandex.market.core.payment.TransactionType;
import ru.yandex.market.mbi.util.MbiMatchers;

public class YtPayoutDtoMatcher {
    private YtPayoutDtoMatcher() {
    }

    public static Matcher<YtPayoutDto> hasPayoutId(Long expectedValue) {
        return MbiMatchers.<YtPayoutDto>newAllOfBuilder()
                .add(YtPayoutDto::getPayoutId, expectedValue, "payoutId")
                .build();
    }

    public static Matcher<YtPayoutDto> hasOrderId(Long expectedValue) {
        return MbiMatchers.<YtPayoutDto>newAllOfBuilder()
                .add(YtPayoutDto::getOrderId, expectedValue, "orderId")
                .build();
    }

    public static Matcher<YtPayoutDto> hasEntityId(Long expectedValue) {
        return MbiMatchers.<YtPayoutDto>newAllOfBuilder()
                .add(YtPayoutDto::getEntityId, expectedValue, "entityId")
                .build();
    }

    public static Matcher<YtPayoutDto> hasCheckouterId(Long expectedValue) {
        return MbiMatchers.<YtPayoutDto>newAllOfBuilder()
                .add(YtPayoutDto::getCheckouterId, expectedValue, "checkouterId")
                .build();
    }

    public static Matcher<YtPayoutDto> hasPartnerId(Long expectedValue) {
        return MbiMatchers.<YtPayoutDto>newAllOfBuilder()
                .add(YtPayoutDto::getPartnerId, expectedValue, "partnerId")
                .build();
    }

    public static Matcher<YtPayoutDto> hasPayoutGroupId(Long expectedValue) {
        return MbiMatchers.<YtPayoutDto>newAllOfBuilder()
                .add(YtPayoutDto::getPayoutGroupId, expectedValue, "payoutGroupId")
                .build();
    }

    public static Matcher<YtPayoutDto> hasPaysysPartnerId(Long expectedValue) {
        return MbiMatchers.<YtPayoutDto>newAllOfBuilder()
                .add(YtPayoutDto::getPaysysPartnerId, expectedValue, "paysysPartnerId")
                .build();
    }

    public static Matcher<YtPayoutDto> hasAmount(Long expectedValue) {
        return MbiMatchers.<YtPayoutDto>newAllOfBuilder()
                .add(YtPayoutDto::getAmount, expectedValue, "amount")
                .build();
    }

    public static Matcher<YtPayoutDto> hasCreatedAt(Instant expectedValue) {
        return MbiMatchers.<YtPayoutDto>newAllOfBuilder()
                .add(YtPayoutDto::getCreatedAt, expectedValue, "createdAt")
                .build();
    }

    public static Matcher<YtPayoutDto> hasCurrency(PaymentOrderCurrency expectedValue) {
        return MbiMatchers.<YtPayoutDto>newAllOfBuilder()
                .add(YtPayoutDto::getCurrency, expectedValue, "currency")
                .build();
    }

    public static Matcher<YtPayoutDto> hasPaysysType(PaysysTypeCc expectedValue) {
        return MbiMatchers.<YtPayoutDto>newAllOfBuilder()
                .add(YtPayoutDto::getPaysysType, expectedValue, "paysysTypeCc")
                .build();
    }

    public static Matcher<YtPayoutDto> hasProductType(PayoutProductType expectedValue) {
        return MbiMatchers.<YtPayoutDto>newAllOfBuilder()
                .add(YtPayoutDto::getPayoutProductType, expectedValue, "payoutProductType")
                .build();
    }

    public static Matcher<YtPayoutDto> hasTransactionType(TransactionType expectedValue) {
        return MbiMatchers.<YtPayoutDto>newAllOfBuilder()
                .add(YtPayoutDto::getTransactionType, expectedValue, "transactionType")
                .build();
    }

    public static Matcher<YtPayoutDto> hasTrantime(Instant expectedValue) {
        return MbiMatchers.<YtPayoutDto>newAllOfBuilder()
                .add(YtPayoutDto::getTrantime, expectedValue, "trantime")
                .build();
    }

    public static Matcher<YtPayoutDto> hasEntityType(EntityType expectedValue) {
        return MbiMatchers.<YtPayoutDto>newAllOfBuilder()
                .add(YtPayoutDto::getEntityType, expectedValue, "entityType")
                .build();
    }

    public static Matcher<YtPayoutDto> hasOrgId(OperatingUnit expectedValue) {
        return MbiMatchers.<YtPayoutDto>newAllOfBuilder()
                .add(YtPayoutDto::getOrgId, expectedValue, "orgId")
                .build();
    }
}
