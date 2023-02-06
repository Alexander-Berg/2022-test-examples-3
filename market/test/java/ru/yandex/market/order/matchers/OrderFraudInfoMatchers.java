package ru.yandex.market.order.matchers;

import java.time.LocalDate;

import org.hamcrest.Matcher;

import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.order.model.OrderFraudInfo;

public class OrderFraudInfoMatchers {

    public static Matcher<OrderFraudInfo> hasOrderId(Long expectedValue) {
        return MbiMatchers.<OrderFraudInfo>newAllOfBuilder()
                .add(OrderFraudInfo::getOrderId, expectedValue, "orderId")
                .build();
    }

    public static Matcher<OrderFraudInfo> isOrderFraud(Boolean expectedValue) {
        return MbiMatchers.<OrderFraudInfo>newAllOfBuilder()
                .add(OrderFraudInfo::isOrderFraud, expectedValue, "orderFraud")
                .build();
    }

    public static Matcher<OrderFraudInfo> hasBuyerUid(Long expectedValue) {
        return MbiMatchers.<OrderFraudInfo>newAllOfBuilder()
                .add(OrderFraudInfo::getBuyerUid, expectedValue, "buyerUid")
                .build();
    }

    public static Matcher<OrderFraudInfo> isFirstOrder(Boolean expectedValue) {
        return MbiMatchers.<OrderFraudInfo>newAllOfBuilder()
                .add(OrderFraudInfo::isFirstOrder, expectedValue, "firstOrder")
                .build();
    }

    public static Matcher<OrderFraudInfo> hasTrafficType(String expectedValue) {
        return MbiMatchers.<OrderFraudInfo>newAllOfBuilder()
                .add(OrderFraudInfo::getTrafficType, expectedValue, "trafficType")
                .build();
    }

    public static Matcher<OrderFraudInfo> isOverLimit(Boolean expectedValue) {
        return MbiMatchers.<OrderFraudInfo>newAllOfBuilder()
                .add(OrderFraudInfo::isOverLimit, expectedValue, "overLimit")
                .build();
    }

    public static Matcher<OrderFraudInfo> hasUpdatedAt(LocalDate expectedValue) {
        return MbiMatchers.<OrderFraudInfo>newAllOfBuilder()
                .add(OrderFraudInfo::getUpdatedAt, expectedValue, "updatedAt")
                .build();
    }
}
