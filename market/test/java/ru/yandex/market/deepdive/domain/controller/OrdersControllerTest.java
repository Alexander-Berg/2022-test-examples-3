package ru.yandex.market.deepdive.domain.controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepdive.domain.order.Orders;
import ru.yandex.market.deepdive.domain.order.OrdersService;
import ru.yandex.market.deepdive.domain.order.PaymentType;
import ru.yandex.market.deepdive.domain.order.Status;


public class OrdersControllerTest {
    @Test
    public void testAllOrdersAfterFilter() {
        OrdersService ordersService = Mockito.mock(OrdersService.class);
        List<Orders> list = Stream.of(
                        Orders.builder().id(228L)
                                .paymentType(PaymentType.CARD)
                                .totalPrice(480L)
                                .deliveryDate(Date.from(Instant.now()))
                                .status(Status.CREATED),
                        Orders.builder().id(229L)
                                .paymentType(PaymentType.CARD)
                                .totalPrice(876L)
                                .deliveryDate(Date.from(Instant.now()))
                                .status(Status.CREATED),
                        Orders.builder().id(230L)
                                .paymentType(PaymentType.CARD)
                                .totalPrice(228L)
                                .deliveryDate(Date.from(Instant.now()))
                                .status(Status.CREATED)
                )
                .map(Orders.OrdersBuilder::build)
                .collect(Collectors.toCollection(ArrayList::new));
        OrdersController ordersController = new OrdersController(ordersService);
        Mockito.when(ordersService.findAllByStatusAndPaymentMethod("CREATED", "CARD"))
                .thenReturn(list);
        Assert.assertEquals(ordersController.allOrdersAfterFilter("CREATED", "CARD"), list);
    }

}
