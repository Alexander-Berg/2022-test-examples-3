package ru.yandex.market.deepdive;

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
import ru.yandex.market.deepdive.domain.pvz_order.PvzOrderRepository;
import ru.yandex.market.deepdive.domain.pvz_order.PvzOrderService;

public class PvzOrderServiceTest {
    @Test
    public void simpleTest() {
        PvzOrderRepository repo = Mockito.mock(PvzOrderRepository.class);
        PvzOrderMapper mapper = new PvzOrderMapper();
        PvzOrderService service = new PvzOrderService(repo, mapper);
        List<PvzOrder> orders = List.of(
                PvzOrder.builder()
                        .id(228L)
                        .pickupPoint(PickupPoint.builder().id(228L).build())
                        .status(PvzOrder.Status.valueOf("CREATED"))
                        .deliveryDate(Date.valueOf("2001-9-11"))
                        .paymentType(PvzOrder.PaymentType.valueOf("CARD"))
                        .totalPrice(1337L)
                        .build());

        Mockito.when(repo.findAll()).thenReturn(orders);
        List<PvzOrderDto> expected = orders.stream().map(mapper::map).collect(Collectors.toList());

        Assert.assertEquals(expected, service.getAll());
    }
}
