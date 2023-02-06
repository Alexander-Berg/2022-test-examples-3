package ru.yandex.market.deepdive.pzv_order;

import java.sql.Date;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.deepdive.domain.client.dto.PvzIntOrderDto;
import ru.yandex.market.deepdive.domain.controller.dto.PvzOrderDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;
import ru.yandex.market.deepdive.domain.pvz_order.PvzOrder;
import ru.yandex.market.deepdive.domain.pvz_order.PvzOrderMapper;
import ru.yandex.market.deepdive.domain.pvz_order.PvzOrderPaymentType;
import ru.yandex.market.deepdive.domain.pvz_order.PvzOrderStatus;

public class PvzOrderMapperTest {
    private final PvzOrderMapper mapper = new PvzOrderMapper();

    @Test
    public void orderToPvzDto() {
        PvzOrder order = PvzOrder.builder()
                .id(15)
                .pickupPoint(PickupPoint.builder().id(110L).build())
                .status(PvzOrderStatus.ARRIVED_TO_PICKUP_POINT)
                .deliveryDate(Date.valueOf("2020-12-30"))
                .paymentType(PvzOrderPaymentType.CARD)
                .build();

        PvzOrderDto expected = PvzOrderDto.builder()
                .id(15)
                .pickupPointId(110)
                .status(PvzOrderStatus.ARRIVED_TO_PICKUP_POINT)
                .deliveryDate(Date.valueOf("2020-12-30"))
                .paymentType(PvzOrderPaymentType.CARD)
                .build();

        Assert.assertEquals(expected, mapper.map(order));
    }

    @Test
    public void pvzIntDtoToOrder() {
        PvzIntOrderDto orderDto = new PvzIntOrderDto();
        orderDto.setId(134);
        orderDto.setPickupPointId(10);
        orderDto.setStatus(PvzOrderStatus.DELIVERED_TO_RECIPIENT);
        orderDto.setDeliveryDate(Date.valueOf("2021-2-30"));
        orderDto.setPaymentType(PvzOrderPaymentType.CASH);

        PvzOrder expected = PvzOrder.builder()
                .id(134)
                .pickupPoint(PickupPoint.builder().id(10L).build())
                .status(PvzOrderStatus.DELIVERED_TO_RECIPIENT)
                .deliveryDate(Date.valueOf("2021-2-30"))
                .paymentType(PvzOrderPaymentType.CASH)
                .build();

        Assert.assertEquals(expected, mapper.map(orderDto));
    }
}
