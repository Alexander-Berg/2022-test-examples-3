package ru.yandex.market.deepdive.pzv_order;


import java.sql.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepdive.domain.controller.dto.PvzOrderDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;
import ru.yandex.market.deepdive.domain.pvz_order.PvzOrder;
import ru.yandex.market.deepdive.domain.pvz_order.PvzOrderMapper;
import ru.yandex.market.deepdive.domain.pvz_order.PvzOrderPaymentType;
import ru.yandex.market.deepdive.domain.pvz_order.PvzOrderRepository;
import ru.yandex.market.deepdive.domain.pvz_order.PvzOrderService;
import ru.yandex.market.deepdive.domain.pvz_order.PvzOrderStatus;

public class PvzOrderServiceTest {

    @Test
    public void getAll() {
        PvzOrderRepository pvzOrderRepository = Mockito.mock(PvzOrderRepository.class);
        PvzOrderMapper mapper = new PvzOrderMapper();
        PvzOrderService pvzOrderService = new PvzOrderService(pvzOrderRepository, mapper);

        List<PvzOrder> orders = List.of(
                PvzOrder.builder()
                        .id(134)
                        .pickupPoint(PickupPoint.builder().id(10L).build())
                        .status(PvzOrderStatus.DELIVERED_TO_RECIPIENT)
                        .deliveryDate(Date.valueOf("2021-2-30"))
                        .paymentType(PvzOrderPaymentType.CARD)
                        .build(),
                PvzOrder.builder()
                        .id(13)
                        .pickupPoint(PickupPoint.builder().id(1L).build())
                        .status(PvzOrderStatus.DELIVERED_TO_RECIPIENT)
                        .deliveryDate(Date.valueOf("2010-2-30"))
                        .paymentType(PvzOrderPaymentType.CARD)
                        .build()
                );

        Mockito.when(pvzOrderRepository.findAll()).thenReturn(orders);
        List<PvzOrderDto> expected = orders.stream().map(mapper::map).collect(Collectors.toList());

        Assert.assertEquals(expected, pvzOrderService.getAll());
    }
}
