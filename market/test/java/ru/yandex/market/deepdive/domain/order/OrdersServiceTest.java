package ru.yandex.market.deepdive.domain.order;

import java.sql.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepdive.domain.controller.dto.OrdersDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;

public class OrdersServiceTest {
    @Test
    public void serviceTest() {
        OrdersRepository repo = Mockito.mock(OrdersRepository.class);
        OrdersMapper mapper = new OrdersMapper();
        OrdersService service = new OrdersService(repo, mapper);
        List<Orders> orders = List.of(
                Orders.builder()
                        .id(777L)
                        .pickupPoint(PickupPoint.builder().id(228L).build())
                        .status(Status.valueOf("CREATED"))
                        .deliveryDate(Date.valueOf("2021-09-30"))
                        .paymentType(PaymentType.valueOf("CASH"))
                        .totalPrice(999L)
                        .build()
        );
        Mockito.when(repo.findAll()).thenReturn(orders);
        List<OrdersDto> expected = orders.stream().map(mapper::map).collect(Collectors.toList());
        Assert.assertEquals(expected, service.findAll());
    }
}
