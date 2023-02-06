package ru.yandex.market.delivery.deliveryintegrationtests.ffwfapi.dto;

import lombok.Data;

@Data
public class OrderDto {

    private long orderId;
    private long shopId;
    private Long shopShipmentId;
    private long deliveryServiceId;
    private boolean fakeOrder;

    public OrderDto(long orderId, long shopId, long deliveryServiceId, boolean fakeOrder) {
        this.orderId = orderId;
        this.shopId = shopId;
        this.deliveryServiceId = deliveryServiceId;
        this.fakeOrder = fakeOrder;
    }
}
