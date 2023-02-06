package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.user.order.checkout.PaymentPartition;
import ru.yandex.market.checkout.checkouter.pay.PaymentAgent;

import java.math.BigDecimal;

/**
 * Created by fettsery on 12.02.19.
 */
public class PaymentPartitionMatcher {
    public static Matcher<PaymentPartition> paymentPartition(Matcher<PaymentPartition>... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<PaymentPartition> paymentAgent(PaymentAgent paymentAgent) {
        return ApiMatchers.map(
            PaymentPartition::getPaymentAgent,
            "'totalAmount'",
            Matchers.is(paymentAgent),
            PaymentPartitionMatcher::toStr
        );
    }

    public static Matcher<PaymentPartition> amount(BigDecimal amount) {
        return ApiMatchers.map(
            PaymentPartition::getAmount,
            "'amount'",
            Matchers.is(amount),
            PaymentPartitionMatcher::toStr
        );
    }

    public static String toStr(PaymentPartition paymentPartition) {
        if (null == paymentPartition) {
            return "null";
        }
        return MoreObjects.toStringHelper(PaymentPartition.class)
            .add("paymentAgent", paymentPartition.getPaymentAgent())
            .add("amount", paymentPartition.getAmount())
            .toString();
    }
}
