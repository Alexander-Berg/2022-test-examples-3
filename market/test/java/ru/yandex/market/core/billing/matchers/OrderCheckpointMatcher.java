package ru.yandex.market.core.billing.matchers;

import java.time.LocalDateTime;

import org.hamcrest.Matcher;

import ru.yandex.market.core.order.model.CheckpointType;
import ru.yandex.market.core.order.model.OrderCheckpoint;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.mbi.util.MbiMatchers;

public class OrderCheckpointMatcher {

    public static Matcher<OrderCheckpoint> hasOrderId(long expectedValue) {
        return MbiMatchers.<OrderCheckpoint>newAllOfBuilder()
                .add(OrderCheckpoint::getOrderId, expectedValue, "orderId")
                .build();
    }

    public static Matcher<OrderCheckpoint> hasCheckpointType(CheckpointType expectedValue) {
        return MbiMatchers.<OrderCheckpoint>newAllOfBuilder()
                .add(OrderCheckpoint::getCheckpointType, expectedValue, "checkpointType")
                .build();
    }

    public static Matcher<OrderCheckpoint> hasDate(LocalDateTime expectedValue) {
        return MbiMatchers.<OrderCheckpoint>newAllOfBuilder()
                .add(OrderCheckpoint::getDate, expectedValue, "date")
                .build();
    }

    public static Matcher<OrderCheckpoint> hasId(long expectedValue) {
        return MbiMatchers.<OrderCheckpoint>newAllOfBuilder()
                .add(OrderCheckpoint::getId, expectedValue, "id")
                .build();
    }

    public static Matcher<OrderCheckpoint> hasPartnerType(PartnerType expectedValue) {
        return MbiMatchers.<OrderCheckpoint>newAllOfBuilder()
                .add(OrderCheckpoint::getPartnerType, expectedValue, "partnerType")
                .build();
    }

    public static Matcher<OrderCheckpoint> hasPartnerId(long expectedValue) {
        return MbiMatchers.<OrderCheckpoint>newAllOfBuilder()
                .add(OrderCheckpoint::getPartnerId, expectedValue, "partnerId")
                .build();
    }
}
