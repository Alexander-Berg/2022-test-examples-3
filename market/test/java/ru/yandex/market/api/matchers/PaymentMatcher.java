package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.common.currency.Currency;
import ru.yandex.market.api.user.order.checkout.Payment;
import ru.yandex.market.api.user.order.checkout.PaymentPartition;

import java.math.BigDecimal;

import static ru.yandex.market.api.ApiMatchers.collectionToStr;

/**
 * Created by fettsery on 12.02.19.
 */
public class PaymentMatcher {
    public static Matcher<Payment> payment(Matcher<Payment>... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<Payment> totalAmount(BigDecimal totalAmount) {
        return ApiMatchers.map(
            Payment::getTotalAmount,
            "'totalAmount'",
            Matchers.is(totalAmount),
            PaymentMatcher::toStr
        );
    }

    public static Matcher<Payment> currency(Currency currency) {
        return ApiMatchers.map(
            Payment::getCurrency,
            "'currency'",
            Matchers.is(currency),
            PaymentMatcher::toStr
        );
    }

    public static Matcher<Payment> partitions(Matcher<Iterable<PaymentPartition>> partitions) {
        return ApiMatchers.map(
            Payment::getPartitions,
            "'partitions'",
            partitions,
            PaymentMatcher::toStr
        );
    }

    public static String toStr(Payment payment) {
        if (null == payment) {
            return "null";
        }
        return MoreObjects.toStringHelper(Payment.class)
            .add("totalAmount", payment.getTotalAmount())
            .add("currency", payment.getCurrency())
            .add(
                "partitions",
                collectionToStr(payment.getPartitions(), PaymentPartitionMatcher::toStr)
            )
            .toString();
    }
}
