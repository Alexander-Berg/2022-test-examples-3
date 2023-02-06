package ru.yandex.market.antifraud.orders.test.providers;

import java.util.ArrayList;
import java.util.List;

import ru.yandex.market.antifraud.orders.web.dto.checkouter.CartRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.MultiCartRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderBuyerRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderDeliveryRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderItemRequestDto;

@SuppressWarnings("checkstyle:MagicNumber")
public abstract class OrderRequestProvider {

    public static MultiCartRequestDto getOrderRequest() {
        return getPreparedOrderRequestBuilder().build();
    }

    public static MultiCartRequestDto getFulfillmentOrderRequest(List<OrderItemRequestDto> items) {
        return getPreparedOrderRequestBuilder()
            .carts(List.of(
                CartRequestDto.builder()
                    .fulfilment(true)
                    .items(items)
                    .delivery(OrderDeliveryProvider.getEmptyOrderDeliveryRequest())
                    .build()
            ))
            .build();
    }

    public static MultiCartRequestDto.MultiCartRequestDtoBuilder getPreparedOrderRequestBuilder() {
        OrderItemRequestDto item = OrderItemRequestProvider.getOrderItem();
        List<OrderItemRequestDto> items = new ArrayList<>();
        items.add(item);

        OrderDeliveryRequestDto delivery = OrderDeliveryProvider.getEmptyOrderDeliveryRequest();
        OrderBuyerRequestDto buyer = OrderBuyerRequestProvider.getOrderBuyerRequest();

        return MultiCartRequestDto.builder()
                .carts(List.of(
                    CartRequestDto.builder()
                        .items(items)
                        .delivery(delivery)
                        .build()
                ))
                .buyer(buyer);
    }
}
