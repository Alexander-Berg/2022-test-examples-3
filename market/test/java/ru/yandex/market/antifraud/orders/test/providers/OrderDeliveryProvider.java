package ru.yandex.market.antifraud.orders.test.providers;

import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderDeliveryRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderDeliveryType;

public abstract class OrderDeliveryProvider {

    public static OrderDeliveryRequestDto getEmptyOrderDeliveryRequest() {
        return new OrderDeliveryRequestDto(null, null, OrderDeliveryType.UNKNOWN, null);
    }

    public static OrderDeliveryRequestDto getOrderDeliveryRequest() {
        return new OrderDeliveryRequestDto(123L, "123", OrderDeliveryType.DELIVERY, null);
    }

    public static OrderDeliveryRequestDto getOrderDeliveryRequestWithBuyerAddress() {
        return new OrderDeliveryRequestDto(123L, "123", OrderDeliveryType.DELIVERY,
                OrderDeliveryBuyerAddressProvider.getOrderDeliveryBuyerAddress());
    }
}
