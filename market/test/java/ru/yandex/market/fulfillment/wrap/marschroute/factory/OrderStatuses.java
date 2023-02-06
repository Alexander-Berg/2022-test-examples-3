package ru.yandex.market.fulfillment.wrap.marschroute.factory;

import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatus;
import ru.yandex.market.logistic.api.utils.DateTime;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class OrderStatuses {

    public static List<OrderStatus> orderStatuses(OrderStatus... orderStatuses) {
        return Arrays.asList(orderStatuses);
    }

    public static OrderStatus orderStatus(OrderStatusType statusType, LocalDateTime dateTime, String message) {
        return new OrderStatus(statusType, DateTime.fromLocalDateTime(dateTime), message);
    }
}
