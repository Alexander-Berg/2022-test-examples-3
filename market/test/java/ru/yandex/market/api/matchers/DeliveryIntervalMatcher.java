package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;

import ru.yandex.market.api.user.order.DeliveryInterval;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.api.ApiMatchers.map;

/**
 * Created by fettsery on 17.05.18.
 */
public class DeliveryIntervalMatcher {
    public static Matcher<DeliveryInterval> deliveryInterval(LocalTime fromTime, LocalTime toTime, boolean isDefault, BigDecimal price) {
        return allOf(
                map(
                        DeliveryInterval::getFromTime,
                        "'fromTime'",
                        is(fromTime),
                        DeliveryIntervalMatcher::toStr
                ),
                map(
                        DeliveryInterval::getToTime,
                        "'toTime'",
                        is(toTime),
                        DeliveryIntervalMatcher::toStr
                ),
                map(
                        DeliveryInterval::isDefault,
                        "'isDefault'",
                        is(isDefault),
                        DeliveryIntervalMatcher::toStr
                ),
                map(
                        DeliveryInterval::getPrice,
                        "'price'",
                        is(price),
                        DeliveryIntervalMatcher::toStr
                )
        );
    }

    public static String toStr(DeliveryInterval deliveryInterval) {
        if (null == deliveryInterval) {
            return "null";
        }
        return MoreObjects.toStringHelper(DeliveryInterval.class)
                .add("fromTime", deliveryInterval.getFromTime())
                .add("toTime", deliveryInterval.getToTime())
                .add("isDefault", deliveryInterval.isDefault())
                .add("price", deliveryInterval.getPrice())
                .toString();
    }
}
