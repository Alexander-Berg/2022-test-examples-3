package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.domain.v2.DiscountType;
import ru.yandex.market.api.domain.v2.cart.DeliveryInfo;

import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.*;
import static ru.yandex.market.api.ApiMatchers.map;

public class SummaryMatcher {

    public static Matcher<DeliveryInfo> deliveryIsFree() {
        return deliveryIsFree(Matchers.nullValue(DiscountType.class));
    }

    public static Matcher<DeliveryInfo> deliveryIsFree(Matcher<DiscountType> type) {
        return allOf(
            map(
                DeliveryInfo::isFree,
                "'isFree'",
                is(true),
                SummaryMatcher::toStr
            ),
            map(
                DeliveryInfo::getPrice,
                "'price'",
                nullValue(),
                SummaryMatcher::toStr
            ),
            map(
                DeliveryInfo::getLeftToFree,
                "'leftToFree'",
                nullValue(),
                SummaryMatcher::toStr
            ),
            map(
                DeliveryInfo::getDiscountType,
                "'discountType'",
                type,
                SummaryMatcher::toStr
            )
        );
    }

    public static Matcher<DeliveryInfo> deliveryCost(BigDecimal value) {
        return allOf(
            map(
                DeliveryInfo::isFree,
                "'isFree'",
                is(false),
                SummaryMatcher::toStr
            ),
            map(
                DeliveryInfo::getPrice,
                "'price'",
                is(value),
                SummaryMatcher::toStr
            )
        );
    }

    public static Matcher<DeliveryInfo> deliveryCost(BigDecimal value, BigDecimal leftToFree) {
        return allOf(
            map(
                DeliveryInfo::isFree,
                "'isFree'",
                is(false),
                SummaryMatcher::toStr
            ),
            map(
                DeliveryInfo::getPrice,
                "'price'",
                is(value),
                SummaryMatcher::toStr
            ),
            map(
                DeliveryInfo::getLeftToFree,
                "'leftToFree'",
                is(leftToFree),
                SummaryMatcher::toStr
            ),
            map(
                DeliveryInfo::getDiscountType,
                "'discountType'",
                Matchers.is(DiscountType.THRESHOLD),
                SummaryMatcher::toStr
            )
        );
    }

    private static String toStr(DeliveryInfo deliveryInfo) {
        if (null == deliveryInfo) {
            return "null";
        }

        return MoreObjects.toStringHelper(DeliveryInfo.class)
            .add("price", deliveryInfo.getPrice())
            .add("isFree", deliveryInfo.isFree())
            .add("leftToFree", deliveryInfo.getLeftToFree())
            .add("discountType", deliveryInfo.getDiscountType())
            .toString();
    }
}
